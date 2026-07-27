package org.example.padelback.modules.pagos.application;

import java.util.Set;

import org.example.padelback.infrastructure.tenancy.TenantContext;
import org.example.padelback.modules.pagos.domain.model.PagoMp;
import org.example.padelback.modules.pagos.domain.model.SenaPago;
import org.example.padelback.modules.pagos.domain.port.CredencialMpStorePort;
import org.example.padelback.modules.pagos.domain.port.MercadoPagoGatewayPort;
import org.example.padelback.modules.pagos.domain.port.SenaPagoStorePort;
import org.example.padelback.modules.reservas.domain.exception.SenaNoValidableException;
import org.example.padelback.modules.reservas.domain.exception.TurnoNoEncontradoException;
import org.example.padelback.modules.reservas.domain.port.TurnoCommandPort;
import org.example.padelback.modules.tenant.infrastructure.PublicTenantResolver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Procesa la notificación de pago de MP. El webhook NO se confía: la fuente de verdad es
 * re-consultar el pago a la API con el token del propio tenant. Idempotente (MP reintenta).
 * Nunca lanza por problemas de negocio: el webhook debe responder 200 para que MP no re-encole.
 *
 * <p>Estados del pago re-consultado, en tres baldes:
 * <ul>
 *   <li>{@code approved} → confirma la seña (o {@code APROBADO_TARDE} si la reserva ya no es
 *       confirmable). Un approved por monto menor al de la seña se registra {@code RECHAZADO}:
 *       ese pago es terminalmente insuficiente.</li>
 *   <li>Terminales negativos ({@code rejected}, {@code cancelled}, {@code refunded},
 *       {@code charged_back}) → {@code RECHAZADO}.</li>
 *   <li>No terminales ({@code pending}, {@code in_process}, {@code authorized}, desconocidos) →
 *       NO se registra nada: si registráramos, el payment_id quedaría "quemado" para la
 *       idempotencia y la notificación posterior de approved del MISMO pago (Rapipago, tarjeta
 *       in_process→approved) se descartaría.</li>
 * </ul>
 *
 * <p>Una seña en un estado liquidado ({@code APROBADO}, {@code APROBADO_TARDE} o {@code DEVUELTO})
 * nunca se degrada: notificaciones tardías (rechazo de un primer intento de tarjeta llegando
 * después del approved) o un segundo pago duplicado se ignoran.
 */
@Service
@RequiredArgsConstructor
public class ProcesarWebhookMpUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcesarWebhookMpUseCase.class);

    /** Estados de pago que ya no pueden transicionar a approved. */
    private static final Set<String> ESTADOS_TERMINALES_NEGATIVOS =
            Set.of("rejected", "cancelled", "refunded", "charged_back");

    /** Estados de la seña ya liquidados: ninguna notificación posterior puede pisarlos. */
    private static final Set<String> ESTADOS_SENA_LIQUIDADOS =
            Set.of(SenaPago.APROBADO, SenaPago.APROBADO_TARDE, SenaPago.DEVUELTO);

    private final PublicTenantResolver tenantResolver;
    private final CredencialMpStorePort credenciales;
    private final MercadoPagoGatewayPort gateway;
    private final SenaPagoStorePort senaPagos;
    private final TurnoCommandPort turnoCommand;

    public void ejecutar(String tenantSlug, String type, String paymentId) {
        if (!"payment".equalsIgnoreCase(type) || paymentId == null || paymentId.isBlank()) {
            return; // otros tópicos no nos interesan
        }
        Long tenantId = tenantResolver.resolve(tenantSlug, null).orElse(null);
        if (tenantId == null) {
            log.warn("Webhook MP con tenant desconocido: {}", tenantSlug);
            return;
        }
        if (senaPagos.pagoYaProcesado(paymentId)) {
            return; // reintento de MP
        }
        var cred = credenciales.cargar(tenantId).orElse(null);
        if (cred == null) {
            log.warn("Webhook MP para tenant {} sin credencial", tenantId);
            return;
        }

        PagoMp pago = gateway.consultarPago(cred.accessToken(), paymentId);
        String[] ref = pago.externalReference() != null ? pago.externalReference().split("-", 2) : new String[0];
        if (ref.length != 2 || !ref[0].equals(String.valueOf(tenantId))) {
            log.warn("Webhook MP: external_reference {} no corresponde al tenant {}",
                    pago.externalReference(), tenantId);
            return;
        }
        final long reservaId;
        try {
            reservaId = Long.parseLong(ref[1]);
        } catch (NumberFormatException e) {
            log.warn("Webhook MP: external_reference {} con reservaId inválido", pago.externalReference());
            return; // igual 200: no es una notificación que podamos procesar
        }

        TenantContext.runAs(tenantId, () -> {
            var senaPago = senaPagos.cargarPorReserva(tenantId, reservaId).orElse(null);
            if (senaPago == null) {
                log.warn("Webhook MP: pago {} sin preferencia registrada (reserva {})", paymentId, reservaId);
                return null;
            }
            if (ESTADOS_SENA_LIQUIDADOS.contains(senaPago.estado())) {
                // Ningún estado liquidado (APROBADO/APROBADO_TARDE/DEVUELTO) puede ser pisado por una
                // notificación tardía: protege el payment_id de la plata que el club retiene y la
                // auditoría de devoluciones.
                log.info("Webhook MP: pago {} ignorado, la seña de la reserva {} ya está {}",
                        paymentId, reservaId, senaPago.estado());
                return null;
            }
            String status = pago.status() == null ? "" : pago.status().toLowerCase();
            if (!"approved".equals(status)) {
                if (ESTADOS_TERMINALES_NEGATIVOS.contains(status)) {
                    senaPagos.registrarPago(tenantId, reservaId, paymentId, SenaPago.RECHAZADO);
                } else {
                    // pending / in_process / authorized / desconocido: no registrar nada, así el
                    // approved posterior del MISMO payment_id sigue siendo procesable.
                    log.info("Webhook MP: pago {} en estado no terminal '{}' (reserva {}), sin efecto",
                            paymentId, pago.status(), reservaId);
                }
                return null;
            }
            if (pago.transactionAmount().compareTo(senaPago.monto()) < 0) {
                log.warn("Webhook MP: pago {} por monto menor ({} < {})", paymentId,
                        pago.transactionAmount(), senaPago.monto());
                senaPagos.registrarPago(tenantId, reservaId, paymentId, SenaPago.RECHAZADO);
                return null;
            }
            try {
                turnoCommand.confirmarSena(reservaId);
                senaPagos.registrarPago(tenantId, reservaId, paymentId, SenaPago.APROBADO);
                log.info("Seña de reserva {} confirmada automáticamente (pago MP {})", reservaId, paymentId);
            } catch (SenaNoValidableException | TurnoNoEncontradoException e) {
                // Pagó pero la reserva ya venció/cambió/se borró: queda para resolución manual
                // del dueño. Nunca 500: MP re-encolaría el webhook en loop.
                senaPagos.registrarPago(tenantId, reservaId, paymentId, SenaPago.APROBADO_TARDE);
                log.warn("Pago MP {} llegó tarde para la reserva {}: {}", paymentId, reservaId, e.getMessage());
            }
            return null;
        });
    }
}
