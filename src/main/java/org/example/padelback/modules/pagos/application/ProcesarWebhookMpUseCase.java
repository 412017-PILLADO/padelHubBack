package org.example.padelback.modules.pagos.application;

import org.example.padelback.infrastructure.tenancy.TenantContext;
import org.example.padelback.modules.pagos.domain.model.PagoMp;
import org.example.padelback.modules.pagos.domain.model.SenaPago;
import org.example.padelback.modules.pagos.domain.port.CredencialMpStorePort;
import org.example.padelback.modules.pagos.domain.port.MercadoPagoGatewayPort;
import org.example.padelback.modules.pagos.domain.port.SenaPagoStorePort;
import org.example.padelback.modules.reservas.domain.exception.SenaNoValidableException;
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
 */
@Service
@RequiredArgsConstructor
public class ProcesarWebhookMpUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcesarWebhookMpUseCase.class);

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
            if (!"approved".equalsIgnoreCase(pago.status())) {
                senaPagos.registrarPago(tenantId, reservaId, paymentId, SenaPago.RECHAZADO);
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
            } catch (SenaNoValidableException e) {
                // Pagó pero la reserva ya venció/cambió: queda para resolución manual del dueño.
                senaPagos.registrarPago(tenantId, reservaId, paymentId, SenaPago.APROBADO_TARDE);
                log.warn("Pago MP {} llegó tarde para la reserva {}: {}", paymentId, reservaId, e.getMessage());
            }
            return null;
        });
    }
}
