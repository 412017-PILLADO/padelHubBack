package org.example.padelback.modules.reservas.domain.model.reserva;

import java.time.LocalDateTime;

public record NuevaReserva(
        Long complejoId,
        Long canchaId,
        LocalDateTime inicio,
        LocalDateTime fin,
        int duracionMinutos,
        String clienteNombre,
        String clienteWhatsapp,
        String ip) {}
