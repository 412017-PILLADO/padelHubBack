package org.example.padelback.modules.reservas.domain.port;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.example.padelback.modules.reservas.domain.model.disponibilidad.AgendaDelDia;

public interface AgendaQueryPort {

    /** Ids de complejos activos del tenant actual (para resolver el complejo por defecto). */
    List<Long> complejosActivos();

    /** true si el complejo existe, pertenece al tenant actual y está activo. */
    boolean complejoActivoExiste(Long complejoId);

    /**
     * Carga la agenda del día: config de duración (paso, duraciones permitidas, default), franjas
     * de apertura compartidas y cada cancha activa con sus ocupaciones (reservas + bloqueos).
     * Optional.empty() si el complejo no existe/está inactivo.
     */
    Optional<AgendaDelDia> cargarAgendaDelDia(Long complejoId, LocalDate fecha);
}
