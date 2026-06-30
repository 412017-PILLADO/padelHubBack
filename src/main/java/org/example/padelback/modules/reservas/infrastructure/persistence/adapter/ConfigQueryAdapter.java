package org.example.padelback.modules.reservas.infrastructure.persistence.adapter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.example.padelback.domain.port.TenantProvider;
import org.example.padelback.modules.reservas.domain.model.CanchaEstado;
import org.example.padelback.modules.reservas.domain.model.ComplejoEstado;
import org.example.padelback.modules.reservas.domain.model.config.ConfigPublico;
import org.example.padelback.modules.reservas.domain.model.config.ConfigPublico.CanchaInfo;
import org.example.padelback.modules.reservas.domain.model.config.ConfigPublico.ComplejoInfo;
import org.example.padelback.modules.reservas.domain.model.config.ConfigPublico.HorarioInfo;
import org.example.padelback.modules.reservas.domain.model.config.ConfigPublico.TenantInfo;
import org.example.padelback.modules.reservas.domain.port.ConfigQueryPort;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.ComplejoJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.CanchaJpaRepository;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.ComplejoJpaRepository;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.HorarioComplejoJpaRepository;
import org.example.padelback.modules.tenant.infrastructure.persistence.entity.TenantJpaEntity;
import org.example.padelback.modules.tenant.infrastructure.persistence.repository.TenantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class ConfigQueryAdapter implements ConfigQueryPort {

    private final TenantProvider tenantProvider;
    private final TenantJpaRepository tenantRepo;
    private final ComplejoJpaRepository complejoRepo;
    private final CanchaJpaRepository canchaRepo;
    private final HorarioComplejoJpaRepository horarioRepo;

    @Override
    @Transactional(readOnly = true)
    public List<Long> complejosActivos() {
        Long tenantId = tenantProvider.requireTenantId();
        return complejoRepo.findByTenantIdAndEstadoAndActiveTrue(tenantId, ComplejoEstado.ACTIVO)
                .stream().map(ComplejoJpaEntity::getId).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConfigPublico> cargar(Long complejoId) {
        Long tenantId = tenantProvider.requireTenantId();

        ComplejoJpaEntity complejo = complejoRepo.findByTenantIdAndIdAndActiveTrue(tenantId, complejoId).orElse(null);
        if (complejo == null || complejo.getEstado() != ComplejoEstado.ACTIVO) {
            return Optional.empty();
        }
        TenantJpaEntity tenant = tenantRepo.findById(tenantId).orElseThrow();

        List<CanchaInfo> canchas = canchaRepo
                .findByTenantIdAndComplejoIdAndEstadoAndActiveTrueOrderByOrdenAsc(tenantId, complejo.getId(), CanchaEstado.ACTIVO)
                .stream()
                .map(c -> new CanchaInfo(c.getId(), c.getNombre(), c.getOrden(), c.isTechada(),
                        c.getTipoPared().name(), c.getPrecioHora(), c.getColor()))
                .toList();

        List<HorarioInfo> horarios = horarioRepo
                .findByTenantIdAndComplejoIdAndActiveTrue(tenantId, complejo.getId()).stream()
                .map(h -> new HorarioInfo(h.getDiaSemana(), h.getHoraInicio(), h.getHoraFin()))
                .sorted((a, b) -> a.diaSemana() != b.diaSemana()
                        ? Integer.compare(a.diaSemana(), b.diaSemana())
                        : a.horaInicio().compareTo(b.horaInicio()))
                .toList();

        TenantInfo tenantInfo = new TenantInfo(tenant.getName(), tenant.getColorPrimario(),
                tenant.getFuente(), tenant.isMostrarPrecios(), tenant.isRequiereTelefono());
        ComplejoInfo complejoInfo = new ComplejoInfo(complejo.getId(), complejo.getNombre(), complejo.getDireccion(),
                complejo.getTelefono(), complejo.getWhatsapp(), complejo.getMapaUrl(), complejo.getInstagram());

        return Optional.of(new ConfigPublico(tenantInfo, complejoInfo, complejo.getPasoMinutos(),
                parseDuraciones(complejo.getDuracionesPermitidas()), complejo.getDuracionDefault(),
                canchas, horarios));
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
}
