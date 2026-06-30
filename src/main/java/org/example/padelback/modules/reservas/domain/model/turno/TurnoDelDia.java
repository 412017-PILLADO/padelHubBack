package org.example.padelback.modules.reservas.domain.model.turno;

import java.time.LocalDateTime;

/** Un turno (reserva confirmada) tal como lo ve el panel: con cancha, cliente y duración. */
public record TurnoDelDia(
        Long id, LocalDateTime inicio, LocalDateTime fin, String clienteNombre, String clienteWhatsapp,
        String canchaNombre, int duracionMinutos, String estado) {}
