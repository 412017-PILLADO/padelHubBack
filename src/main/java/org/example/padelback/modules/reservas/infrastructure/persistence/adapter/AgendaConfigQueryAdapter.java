package org.example.padelback.modules.reservas.infrastructure.persistence.adapter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.example.padelback.domain.port.TenantProvider;
import org.example.padelback.modules.reservas.domain.exception.ComplejoNoResueltoException;
import org.example.padelback.modules.reservas.domain.model.ComplejoEstado;
import org.example.padelback.modules.reservas.domain.model.config.AgendaConfig;
import org.example.padelback.modules.reservas.domain.port.AgendaConfigQueryPort;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.BloqueoJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.CanchaJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.ComplejoJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.HorarioComplejoJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.PrecioFranjaJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.BloqueoJpaRepository;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.CanchaJpaRepository;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.ComplejoJpaRepository;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.HorarioComplejoJpaRepository;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.PrecioFranjaJpaRepository;
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
    private final PrecioFranjaJpaRepository precioFranjaRepo;
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
                // M-medianoche: "to" es el cierre del día, y "00:00" ahí significa 24:00 (cierra al
                // terminar el día), no la apertura. Con un max() de LocalTime común, 00:00 compararía
                // como el MENOR horario y una franja "20:00-00:00" mostraría "to" = 20:00 (mal). Se
                // sigue devolviendo "00:00" tal cual al panel (así se guardó), pero comparando como si
                // fuera el mayor horario del día.
                LocalTime to = delDia.stream().map(HorarioComplejoJpaEntity::getHoraFin)
                        .max((a, b) -> Integer.compare(comoCierre(a), comoCierre(b)))
                        .orElse(DEFAULT_TO);
                week.add(new AgendaConfig.DiaConfig(dia, true, from, to));
                // break: primer día abierto con exactamente 2 franjas
                if (!breakOn && delDia.size() == 2) {
                    breakOn = true;
                    breakFrom = delDia.get(0).getHoraFin();
                    breakTo = delDia.get(1).getHoraInicio();
                }
            }
        }

        // M1: el panel del dueño muestra TODAS las canchas no eliminadas (ACTIVO e INACTIVO) — si acá
        // se filtrara por ACTIVO, una cancha pasada a INACTIVO desaparecería para siempre del panel y
        // no habría forma de reactivarla. La disponibilidad pública (AgendaQueryAdapter) y el config
        // público (ConfigQueryAdapter) SÍ siguen filtrando solo ACTIVO: ahí es intencional.
        List<CanchaJpaEntity> canchas = canchaRepo.findByTenantIdAndActiveTrue(tenantId).stream()
                .filter(c -> c.getComplejoId().equals(complejo.getId()))
                .sorted(Comparator.comparingInt(CanchaJpaEntity::getOrden))
                .toList();
        Map<Long, String> nombrePorCancha = canchas.stream()
                .collect(Collectors.toMap(CanchaJpaEntity::getId, CanchaJpaEntity::getNombre));

        List<AgendaConfig.CanchaItem> canchaItems = canchas.stream()
                .map(c -> new AgendaConfig.CanchaItem(c.getId(), c.getNombre(), c.getOrden(), c.isTechada(),
                        c.getTipoPared().name(), c.getPrecioHora(), c.getColor(), c.getEstado().name()))
                .toList();

        List<PrecioFranjaJpaEntity> franjasPrecio =
                precioFranjaRepo.findByTenantIdAndComplejoIdAndActiveTrue(tenantId, complejo.getId());
        List<AgendaConfig.PrecioFranjaItem> precioFranjaItems = franjasPrecio.stream()
                .sorted((a, b) -> a.getHoraDesde().compareTo(b.getHoraDesde()))
                .map(f -> new AgendaConfig.PrecioFranjaItem(f.getId(), f.getHoraDesde(), f.getHoraHasta(), f.getPrecioHora()))
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
                complejo.isPermitirOtrasDuraciones(),
                complejo.getPrecioModo().name(),
                complejo.getPrecioHoraGeneral(),
                complejo.isRequiereSena(),
                complejo.getSenaMonto(),
                complejo.getSenaAlias(),
                complejo.isAutoasignacion(),
                breakOn, breakFrom, breakTo,
                week, bloqueoItems, canchaItems, precioFranjaItems, contacto);
    }

    /** Minutos de un horario "de cierre" tratando 00:00 como medianoche (24:00), no como apertura. */
    private static int comoCierre(LocalTime t) {
        return t.equals(LocalTime.MIDNIGHT) ? 24 * 60 : t.toSecondOfDay() / 60;
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
