package org.example.padelback.modules.reservas.domain.model.disponibilidad;

import java.time.LocalTime;

/** Rango horario de apertura del complejo (ej. 08:00-23:00). */
public record Franja(LocalTime inicio, LocalTime fin) {}
