package org.example.padelback.modules.reservas.domain.port;

/**
 * ¿El tenant puede cobrar señas online? Puerto del módulo reservas implementado por el módulo
 * pagos (la dependencia va pagos→reservas): true si hay credencial de Mercado Pago vigente.
 */
public interface PagoOnlineQuery {

    boolean disponible(long tenantId);
}
