package org.example.padelback.modules.reservas.domain.model.reserva;

import java.time.LocalDateTime;

import org.example.padelback.modules.reservas.domain.model.ReservaEstado;

/**
 * @param estado   CONFIRMADO (reserva directa) o PENDIENTE (a la espera de validar la seña).
 * @param expiraEn solo para PENDIENTE: momento en que la reserva deja de retener la cancha. null si CONFIRMADO.
 */
public record NuevaReserva(
        Long complejoId,
        Long canchaId,
        LocalDateTime inicio,
        LocalDateTime fin,
        int duracionMinutos,
        String clienteNombre,
        String clienteWhatsapp,
        String ip,
        ReservaEstado estado,
        LocalDateTime expiraEn) {}
