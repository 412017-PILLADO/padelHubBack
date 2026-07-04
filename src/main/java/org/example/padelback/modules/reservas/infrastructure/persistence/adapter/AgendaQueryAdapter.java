package org.example.padelback.modules.reservas.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.example.padelback.domain.port.TenantProvider;
import org.example.padelback.modules.reservas.domain.model.CanchaEstado;
import org.example.padelback.modules.reservas.domain.model.ComplejoEstado;
import org.example.padelback.modules.reservas.domain.model.PrecioModo;
import org.example.padelback.modules.reservas.domain.model.disponibilidad.AgendaCancha;
import org.example.padelback.modules.reservas.domain.model.disponibilidad.AgendaDelDia;
import org.example.padelback.modules.reservas.domain.model.disponibilidad.CanchaRef;
import org.example.padelback.modules.reservas.domain.model.disponibilidad.Franja;
import org.example.padelback.modules.reservas.domain.model.disponibilidad.Ocupacion;
import org.example.padelback.modules.reservas.domain.port.AgendaQueryPort;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.BloqueoJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.CanchaJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.ComplejoJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.HorarioComplejoJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.ReservaJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.BloqueoJpaRepository;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.CanchaJpaRepository;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.ComplejoJpaRepository;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.HorarioComplejoJpaRepository;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.ReservaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class AgendaQueryAdapter implements AgendaQueryPort {

    private final TenantProvider tenantProvider;
    private final ComplejoJpaRepository complejoRepo;
    private final CanchaJpaRepository canchaRepo;
    private final HorarioComplejoJpaRepository horarioRepo;
    private final ReservaJpaRepository reservaRepo;
    private final BloqueoJpaRepository bloqueoRepo;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<Long> complejosActivos() {
        Long tenantId = tenantProvider.requireTenantId();
        return complejoRepo.findByTenantIdAndEstadoAndActiveTrue(tenantId, ComplejoEstado.ACTIVO)
                .stream().map(ComplejoJpaEntity::getId).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean complejoActivoExiste(Long complejoId) {
        Long tenantId = tenantProvider.requireTenantId();
        return complejoRepo.findByTenantIdAndIdAndActiveTrue(tenantId, complejoId)
                .filter(c -> c.getEstado() == ComplejoEstado.ACTIVO)
                .isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AgendaDelDia> cargarAgendaDelDia(Long complejoId, LocalDate fecha) {
        Long tenantId = tenantProvider.requireTenantId();

        ComplejoJpaEntity complejo = complejoRepo.findByTenantIdAndIdAndActiveTrue(tenantId, complejoId).orElse(null);
        if (complejo == null || complejo.getEstado() != ComplejoEstado.ACTIVO) {
            return Optional.empty();
        }

        // Si el complejo no permite otras duraciones, la única reservable es el turno principal.
        List<Integer> duraciones = complejo.isPermitirOtrasDuraciones()
                ? parseDuraciones(complejo.getDuracionesPermitidas())
                : List.of(complejo.getDuracionDefault());

        // Franjas de apertura (compartidas por todas las canchas) para el día de la semana.
        int diaSemana = fecha.getDayOfWeek().getValue() - 1; // MONDAY=1 -> 0 ... SUNDAY=7 -> 6
        List<Franja> franjas = horarioRepo
                .findByTenantIdAndComplejoIdAndDiaSemanaAndActiveTrue(tenantId, complejoId, diaSemana)
                .stream().map(h -> new Franja(h.getHoraInicio(), h.getHoraFin())).toList();

        List<CanchaJpaEntity> canchas = canchaRepo
                .findByTenantIdAndComplejoIdAndEstadoAndActiveTrueOrderByOrdenAsc(tenantId, complejoId, CanchaEstado.ACTIVO);

        if (canchas.isEmpty() || franjas.isEmpty()) {
            return Optional.of(new AgendaDelDia(fecha, complejo.getPasoMinutos(), duraciones,
                    complejo.getDuracionDefault(), complejo.isRequiereSena(), franjas, List.of()));
        }

        LocalDateTime desde = fecha.atStartOfDay();
        LocalDateTime hasta = fecha.plusDays(1).atStartOfDay();

        // Ocupan el slot las CONFIRMADAS y las PENDIENTE de seña todavía vigentes (expira_en > ahora).
        List<ReservaJpaEntity> reservas = reservaRepo.ocupacionesVigentesDelComplejo(
                tenantId, complejoId, LocalDateTime.now(clock), desde, hasta);
        List<BloqueoJpaEntity> bloqueos = bloqueoRepo
                .findByTenantIdAndComplejoIdAndActiveTrueAndFechaHoraDesdeLessThanAndFechaHoraHastaGreaterThan(
                        tenantId, complejoId, hasta, desde);

        List<AgendaCancha> agendaCanchas = canchas.stream().map(c -> {
            List<Ocupacion> ocupaciones = new ArrayList<>();
            reservas.stream()
                    .filter(r -> r.getCanchaId().equals(c.getId()))
                    .forEach(r -> ocupaciones.add(new Ocupacion(r.getInicio(), r.getFin())));
            // Bloqueos: los de esta cancha + los de complejo entero (cancha_id null).
            bloqueos.stream()
                    .filter(b -> b.getCanchaId() == null || b.getCanchaId().equals(c.getId()))
                    .forEach(b -> ocupaciones.add(new Ocupacion(b.getFechaHoraDesde(), b.getFechaHoraHasta())));
            CanchaRef ref = new CanchaRef(c.getId(), c.getNombre(), c.getColor(), c.isTechada(),
                    c.getTipoPared(), precioEfectivo(complejo, c));
            return new AgendaCancha(ref, ocupaciones);
        }).toList();

        return Optional.of(new AgendaDelDia(fecha, complejo.getPasoMinutos(), duraciones,
                complejo.getDuracionDefault(), complejo.isRequiereSena(), franjas, agendaCanchas));
    }

    /** Precio por hora aplicable a la cancha según el modo del complejo (GENERAL o POR_CANCHA). */
    private static BigDecimal precioEfectivo(ComplejoJpaEntity complejo, CanchaJpaEntity cancha) {
        return complejo.getPrecioModo() == PrecioModo.GENERAL
                ? complejo.getPrecioHoraGeneral()
                : cancha.getPrecioHora();
    }

    /** Parsea el CSV "60,90,120" a una lista de minutos, ignorando valores inválidos. */
    private List<Integer> parseDuraciones(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> s.matches("\\d+"))
                .map(Integer::valueOf)
                .toList();
    }
}
