package org.example.padelback.modules.pagos.domain.model;

import java.math.BigDecimal;

/** Estado del cobro online de la seña de una reserva (preferencia + pago que la saldó). */
public record SenaPago(
        long reservaId,
        String preferenceId,
        String initPoint,
        String paymentId,
        String estado,
        BigDecimal monto) {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String APROBADO = "APROBADO";
    public static final String APROBADO_TARDE = "APROBADO_TARDE";
    public static final String RECHAZADO = "RECHAZADO";
    public static final String DEVUELTO = "DEVUELTO";
}
