package org.example.padelback.modules.reservas.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.example.padelback.modules.reservas.domain.exception.ComplejoNoResueltoException;
import org.example.padelback.modules.reservas.domain.exception.DuracionInvalidaException;
import org.example.padelback.modules.reservas.domain.model.disponibilidad.AgendaDelDia;
import org.example.padelback.modules.reservas.domain.model.disponibilidad.SlotDisponibilidad;
import org.example.padelback.modules.reservas.domain.port.AgendaQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConsultarDisponibilidadUseCase {

    private final AgendaQueryPort agendaQueryPort;
    private final AvailabilityService availabilityService;
    private final Clock clock;

    /**
     * @param complejoId opcional (si el tenant tiene 1 complejo se resuelve solo)
     * @param fecha      día a consultar
     * @param duracion   minutos pedidos; si es null usa la duración default del complejo. Se valida
     *                   contra las duraciones permitidas.
     */
    public List<SlotDisponibilidad> ejecutar(Long complejoId, LocalDate fecha, Integer duracion) {
        Long complejoResuelto = resolverComplejo(complejoId);
        AgendaDelDia agenda = agendaQueryPort.cargarAgendaDelDia(complejoResuelto, fecha)
                .orElseThrow(() -> new ComplejoNoResueltoException("Complejo " + complejoResuelto + " no encontrado"));

        int duracionEfectiva = duracion != null ? duracion : agenda.duracionDefault();
        if (!agenda.duracionesPermitidas().isEmpty() && !agenda.duracionesPermitidas().contains(duracionEfectiva)) {
            throw new DuracionInvalidaException(
                    "Duración " + duracionEfectiva + " no permitida. Opciones: " + agenda.duracionesPermitidas());
        }

        return availabilityService.calcular(agenda, duracionEfectiva, LocalDateTime.now(clock));
    }

    private Long resolverComplejo(Long complejoId) {
        if (complejoId != null) {
            if (!agendaQueryPort.complejoActivoExiste(complejoId)) {
                throw new ComplejoNoResueltoException("Complejo " + complejoId + " no encontrado");
            }
            return complejoId;
        }
        List<Long> activos = agendaQueryPort.complejosActivos();
        if (activos.size() != 1) {
            throw new ComplejoNoResueltoException(
                    "Se requiere complejoId: el tenant tiene " + activos.size() + " complejos activos");
        }
        return activos.get(0);
    }
}
