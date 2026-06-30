package org.example.padelback.modules.reservas.infrastructure.persistence.repository;

import java.util.List;

import org.example.padelback.modules.reservas.infrastructure.persistence.entity.HorarioComplejoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HorarioComplejoJpaRepository extends JpaRepository<HorarioComplejoJpaEntity, Long> {

    List<HorarioComplejoJpaEntity> findByTenantIdAndComplejoIdAndDiaSemanaAndActiveTrue(
            Long tenantId, Long complejoId, int diaSemana);

    List<HorarioComplejoJpaEntity> findByTenantIdAndComplejoIdAndActiveTrue(Long tenantId, Long complejoId);

    void deleteByTenantIdAndComplejoId(Long tenantId, Long complejoId);
}
