package org.example.padelback.modules.reservas.infrastructure.persistence.adapter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.example.padelback.domain.port.TenantProvider;
import org.example.padelback.modules.reservas.domain.exception.ComplejoNoResueltoException;
import org.example.padelback.modules.reservas.domain.model.CanchaEstado;
import org.example.padelback.modules.reservas.domain.model.ComplejoEstado;
import org.example.padelback.modules.reservas.domain.model.config.AgendaConfig;
import org.example.padelback.modules.reservas.domain.port.AgendaConfigQueryPort;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.BloqueoJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.CanchaJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.ComplejoJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.HorarioComplejoJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.BloqueoJpaRepository;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.CanchaJpaRepository;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.ComplejoJpaRepository;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.HorarioComplejoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class AgendaConfigQueryAdapter implements AgendaConfigQueryPort {

    private static final LocalTime DEFAULT_FROM = LocalTime.of(8, 0);
    private static final LocalTime DEFAULT_TO = LocalTime.of(23, 0);
    private static final LocalTime DEFAULT_BREAK_FROM = LocalTime.of(13, 0);
    private static final LocalTime DEFAULT_BREAK_TO = LocalTime.of(16, 0);

    private final TenantProvider tenantProvider;
    private final ComplejoJpaRepository complejoRepo;
    private final CanchaJpaRepository canchaRepo;
    private final HorarioComplejoJpaRepository horarioRepo;
    private final BloqueoJpaRepository bloqueoRepo;

    @Override
    @Transactional(readOnly = true)
    public AgendaConfig cargar() {
        Long tenantId = tenantProvider.requireTenantId();
        ComplejoJpaEntity complejo = resolverComplejo(tenantId);

        List<HorarioComplejoJpaEntity> franjas =
                horarioRepo.findByTenantIdAndComplejoIdAndActiveTrue(tenantId, complejo.getId());

        List<AgendaConfig.DiaConfig> week = new ArrayList<>(7);
        boolean breakOn = false;
        LocalTime breakFrom = DEFAULT_BREAK_FROM;
        LocalTime breakTo = DEFAULT_BREAK_TO;

        for (int dia = 0; dia <= 6; dia++) {
            final int d = dia;
            List<HorarioComplejoJpaEntity> delDia = franjas.stream()
                    .filter(h -> h.getDiaSemana() == d)
                    .sorted((a, b) -> a.getHoraInicio().compareTo(b.getHoraInicio()))
                    .toList();
            if (delDia.isEmpty()) {
                week.add(new AgendaConfig.DiaConfig(dia, false, DEFAULT_FROM, DEFAULT_TO));
            } else {
                LocalTime from = delDia.stream().map(HorarioComplejoJpaEntity::getHoraInicio)
                        .min(LocalTime::compareTo).orElse(DEFAULT_FROM);
                LocalTime to = delDia.stream().map(HorarioComplejoJpaEntity::getHoraFin)
                        .max(LocalTime::compareTo).orElse(DEFAULT_TO);
                week.add(new AgendaConfig.DiaConfig(dia, true, from, to));
                // break: primer día abierto con exactamente 2 franjas
                if (!breakOn && delDia.size() == 2) {
                    breakOn = true;
                    breakFrom = delDia.get(0).getHoraFin();
                    breakTo = delDia.get(1).getHoraInicio();
                }
            }
        }

        List<CanchaJpaEntity> canchas = canchaRepo
                .findByTenantIdAndComplejoIdAndEstadoAndActiveTrueOrderByOrdenAsc(
                        tenantId, complejo.getId(), CanchaEstado.ACTIVO);
        Map<Long, String> nombrePorCancha = canchas.stream()
                .collect(Collectors.toMap(CanchaJpaEntity::getId, CanchaJpaEntity::getNombre));

        List<AgendaConfig.CanchaItem> canchaItems = canchas.stream()
                .map(c -> new AgendaConfig.CanchaItem(c.getId(), c.getNombre(), c.getOrden(), c.isTechada(),
                        c.getTipoPared().name(), c.getPrecioHora(), c.getColor(), c.getEstado().name()))
                .toList();

        LocalDate hoy = LocalDate.now();
        List<BloqueoJpaEntity> bloqueos = bloqueoRepo
                .findByTenantIdAndComplejoIdAndActiveTrueAndFechaHoraHastaGreaterThanEqualOrderByFechaHoraDesdeAsc(
                        tenantId, complejo.getId(), hoy.atStartOfDay());
        List<AgendaConfig.BloqueoItem> bloqueoItems = bloqueos.stream()
                .map(b -> new AgendaConfig.BloqueoItem(
                        b.getId(), b.getFechaHoraDesde().toLocalDate(), b.getCanchaId(),
                        nombreCancha(b.getCanchaId(), nombrePorCancha), b.getMotivo()))
                .toList();

        AgendaConfig.Contacto contacto = new AgendaConfig.Contacto(
                complejo.getDireccion(), complejo.getTelefono(), complejo.getWhatsapp(),
                complejo.getMapaUrl(), complejo.getInstagram());

        return new AgendaConfig(
                complejo.getNombre(),
                complejo.getPasoMinutos(),
                parseDuraciones(complejo.getDuracionesPermitidas()),
                complejo.getDuracionDefault(),
                breakOn, breakFrom, breakTo,
                week, bloqueoItems, canchaItems, contacto);
    }

    private static String nombreCancha(Long canchaId, Map<Long, String> nombrePorCancha) {
        if (canchaId == null) {
            return "Todo el complejo";
        }
        return nombrePorCancha.getOrDefault(canchaId, "—");
    }

    private static List<Integer> parseDuraciones(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> s.matches("\\d+"))
                .map(Integer::valueOf)
                .toList();
    }

    private ComplejoJpaEntity resolverComplejo(Long tenantId) {
        List<ComplejoJpaEntity> complejos =
                complejoRepo.findByTenantIdAndEstadoAndActiveTrue(tenantId, ComplejoEstado.ACTIVO);
        if (complejos.size() != 1) {
            throw new ComplejoNoResueltoException("No hay exactamente un complejo activo configurable");
        }
        return complejos.get(0);
    }
}
