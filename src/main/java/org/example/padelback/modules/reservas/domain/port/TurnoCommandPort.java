package org.example.padelback.modules.reservas.domain.port;

public interface TurnoCommandPort {
    /** Cancela el turno (lo deja en estado CANCELADO, liberando el slot). */
    void cancelar(Long turnoId);

    /** Valida la seña de una reserva PENDIENTE vigente: la pasa a CONFIRMADO. */
    void confirmarSena(Long turnoId);

    /** Rechaza la seña de una reserva PENDIENTE: la pasa a CANCELADO, liberando el slot. */
    void rechazarSena(Long turnoId);
}
