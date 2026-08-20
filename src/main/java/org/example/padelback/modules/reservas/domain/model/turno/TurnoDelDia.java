package org.example.padelback.modules.reservas.domain.model.turno;

import java.time.LocalDateTime;

/**
 * Un turno (reserva confirmada o pendiente vigente) tal como lo ve el panel: con cancha, cliente y
 * duración. Lleva {@code canchaId} además del nombre porque el panel arma la grilla ubicando cada
 * turno en la columna de SU cancha: por nombre, dos canchas homónimas caen en la misma columna.
 */
public record TurnoDelDia(
        Long id, LocalDateTime inicio, LocalDateTime fin, String clienteNombre, String clienteWhatsapp,
        Long canchaId, String canchaNombre, int duracionMinutos, String estado) {}
