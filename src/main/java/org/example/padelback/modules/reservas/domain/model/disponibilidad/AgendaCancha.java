package org.example.padelback.modules.reservas.domain.model.disponibilidad;

import java.util.List;

/** Una cancha con sus tramos ocupados (reservas + bloqueos) del día. */
public record AgendaCancha(CanchaRef cancha, List<Ocupacion> ocupaciones) {}
