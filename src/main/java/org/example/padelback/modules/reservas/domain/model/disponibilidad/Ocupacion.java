package org.example.padelback.modules.reservas.domain.model.disponibilidad;

import java.time.LocalDateTime;

/** Tramo ocupado de una cancha (reserva confirmada o bloqueo). */
public record Ocupacion(LocalDateTime inicio, LocalDateTime fin) {}
