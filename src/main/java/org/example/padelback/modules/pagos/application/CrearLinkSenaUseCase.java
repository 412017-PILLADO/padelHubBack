package org.example.padelback.modules.pagos.application;

import java.time.Clock;
import java.time.LocalDateTime;

import org.example.padelback.domain.port.TenantProvider;
import org.example.padelback.infrastructure.config.MercadoPagoProperties;
import org.example.padelback.modules.pagos.domain.exception.LinkSenaNoDisponibleException;
import org.example.padelback.modules.pagos.domain.exception.MpNoConectadoException;
import org.example.padelback.modules.pagos.domain.model.CredencialMp;
import org.example.padelback.modules.pagos.domain.model.PreferenciaSena;
import org.example.padelback.modules.pagos.domain.model.SenaPago;
import org.example.padelback.modules.pagos.domain.port.CredencialMpStorePort;
import org.example.padelback.modules.pagos.domain.port.MercadoPagoGatewayPort;
import org.example.padelback.modules.pagos.domain.port.SenaPagoStorePort;
import org.example.padelback.modules.tenant.infrastructure.persistence.repository.TenantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Emite (una sola vez) el link de Checkout Pro para pagar la seña de una reserva PENDIENTE.
 * Lo llama el front justo después de crear la reserva; si el tenant no tiene MP conectado el
 * front ni lo intenta (flag pagoOnline en /public/config), pero acá se re-valida igual.
 */
@Service
@RequiredArgsConstructor
public class CrearLinkSenaUseCase {

    private final SenaPagoStorePort senaPagos;
    private final CredencialMpStorePort credenciales;
    private final MercadoPagoGatewayPort gateway;
    private final TenantProvider tenantProvider;
    private final TenantJpaRepository tenantRepo;
    private final MercadoPagoProperties props;
    private final Clock clock;

    public String ejecutar(long reservaId, String backUrl) {
        long tenantId = tenantProvider.requireTenantId();

        // Idempotente: si ya se emitió link para esta reserva, se devuelve el mismo.
        var existente = senaPagos.cargarPorReserva(tenantId, reservaId);
        if (existente.isPresent()) {
            return existente.get().initPoint();
        }

        CredencialMp cred = credenciales.cargar(tenantId)
                .filter(c -> c.vigente(clock.instant()))
                .orElseThrow(MpNoConectadoException::new);

        SenaPagoStorePort.DatosLinkSena datos = senaPagos.datosParaLink(tenantId, reservaId)
                .orElseThrow(() -> new LinkSenaNoDisponibleException("Reserva no encontrada"));
        if (!"PENDIENTE".equals(datos.estadoReserva()) || !datos.requiereSena()
                || datos.senaMonto() == null) {
            throw new LinkSenaNoDisponibleException("La reserva no está esperando seña.");
        }
        if (datos.expiraEn() != null && !datos.expiraEn().isAfter(LocalDateTime.now(clock))) {
            throw new LinkSenaNoDisponibleException("La ventana de pago de la seña ya venció.");
        }

        String slug = tenantRepo.findById(tenantId).orElseThrow().getSlug();
        String notificationUrl = props.webhookBaseUrl() == null || props.webhookBaseUrl().isBlank()
                ? null
                : props.webhookBaseUrl() + "/public/pagos/mp/webhook?tenant=" + slug;
        String externalReference = tenantId + "-" + reservaId;

        PreferenciaSena pref = gateway.crearPreferencia(cred.accessToken(),
                "Seña — " + datos.complejoNombre(), datos.senaMonto(), externalReference,
                notificationUrl, datos.expiraEn(), backUrl);

        senaPagos.guardarPreferencia(tenantId, reservaId, pref.preferenceId(), pref.initPoint(),
                datos.senaMonto());
        // Primera escritura gana (ver SenaPagoStore.guardarPreferencia): si perdimos la carrera
        // releemos para devolver el initPoint que realmente quedó persistido. La preferencia que
        // creamos en MP y no se guardó queda "huérfana" pero es inofensiva: expira sola con la
        // ventana de la reserva.
        return senaPagos.cargarPorReserva(tenantId, reservaId)
                .map(SenaPago::initPoint)
                .orElse(pref.initPoint());
    }
}
