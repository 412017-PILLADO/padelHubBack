package org.example.padelback.modules.pagos.domain.port;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.example.padelback.modules.pagos.domain.model.SenaPago;

public interface SenaPagoStorePort {

    /** Snapshot de la reserva + config de seña del complejo para decidir si se puede emitir link. */
    record DatosLinkSena(String estadoReserva, LocalDateTime expiraEn, BigDecimal senaMonto,
                         boolean requiereSena, String complejoNombre) {}

    Optional<DatosLinkSena> datosParaLink(long tenantId, long reservaId);

    Optional<SenaPago> cargarPorReserva(long tenantId, long reservaId);

    void guardarPreferencia(long tenantId, long reservaId, String preferenceId,
                            String initPoint, BigDecimal monto);

    /** Idempotencia del webhook: MP reintenta la misma notificación varias veces. */
    boolean pagoYaProcesado(String paymentId);

    void registrarPago(long tenantId, long reservaId, String paymentId, String estado);
}
