package org.example.padelback.modules.reservas.domain.model.disponibilidad;

import java.time.LocalDate;
import java.util.List;

/**
 * Snapshot de disponibilidad de un complejo para un día. Las franjas (horario de apertura) son
 * COMPARTIDAS por todas las canchas; el paso define la granularidad de los inicios de turno y las
 * duraciones permitidas/ default son la config de duración variable del complejo.
 */
public record AgendaDelDia(
        LocalDate fecha,
        int pasoMinutos,
        List<Integer> duracionesPermitidas,
        int duracionDefault,
        List<Franja> franjas,
        List<AgendaCancha> canchas) {}
