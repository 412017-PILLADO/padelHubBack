package org.example.padelback.modules.reservas.infrastructure.persistence.adapter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.example.padelback.domain.port.TenantProvider;
import org.example.padelback.modules.reservas.domain.model.ReservaEstado;
import org.example.padelback.modules.reservas.domain.model.turno.TurnoDelDia;
import org.example.padelback.modules.reservas.domain.port.TurnoQueryPort;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.CanchaJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.CanchaJpaRepository;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.ReservaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class TurnoQueryAdapter implements TurnoQueryPort {

    private final TenantProvider tenantProvider;
    private final ReservaJpaRepository reservaRepo;
    private final CanchaJpaRepository canchaRepo;

    @Override
    @Transactional(readOnly = true)
    public List<TurnoDelDia> turnosDelDia(LocalDate fecha) {
        Long tenantId = tenantProvider.requireTenantId();
        LocalDateTime desde = fecha.atStartOfDay();
        LocalDateTime hasta = fecha.plusDays(1).atStartOfDay();

        Map<Long, String> canchas = canchaRepo.findByTenantIdAndActiveTrue(tenantId).stream()
                .collect(Collectors.toMap(CanchaJpaEntity::getId, CanchaJpaEntity::getNombre));

        return reservaRepo
                .findByTenantIdAndEstadoAndActiveTrueAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
                        tenantId, ReservaEstado.CONFIRMADO, desde, hasta)
                .stream()
                .map(r -> new TurnoDelDia(
                        r.getId(), r.getInicio(), r.getFin(), r.getClienteNombre(), r.getClienteWhatsapp(),
                        canchas.getOrDefault(r.getCanchaId(), "—"),
                        r.getDuracionMinutos(),
                        r.getEstado().name()))
                .toList();
    }
}
