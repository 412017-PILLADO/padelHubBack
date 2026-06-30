package org.example.padelback.modules.reservas.domain.model.disponibilidad;

import java.time.LocalTime;
import java.util.List;

/** Un horario de inicio de la grilla, con las canchas libres para la duración pedida. */
public record SlotDisponibilidad(LocalTime hora, boolean disponible, List<CanchaLibre> canchasLibres) {}
