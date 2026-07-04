package org.example.padelback.modules.reservas.domain.model;

public enum ReservaEstado {
    /**
     * Reserva a la espera de que el dueño valide la seña (módulo de señas activado en el complejo).
     * Retiene la cancha mientras {@code expiraEn > ahora}; al vencer, la libera y un job la pasa a
     * {@link #CANCELADO}.
     */
    PENDIENTE,
    CONFIRMADO,
    CANCELADO
}
