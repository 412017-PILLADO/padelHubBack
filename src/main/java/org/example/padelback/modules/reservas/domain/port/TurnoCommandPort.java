package org.example.padelback.modules.reservas.domain.port;

public interface TurnoCommandPort {
    /** Cancela el turno (lo deja en estado CANCELADO, liberando el slot). */
    void cancelar(Long turnoId);
}
