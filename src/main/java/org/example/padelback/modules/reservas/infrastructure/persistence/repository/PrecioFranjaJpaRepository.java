package org.example.padelback.modules.reservas.infrastructure.persistence.repository;

import java.util.List;

import org.example.padelback.modules.reservas.infrastructure.persistence.entity.PrecioFranjaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrecioFranjaJpaRepository extends JpaRepository<PrecioFranjaJpaEntity, Long> {

    List<PrecioFranjaJpaEntity> findByTenantIdAndComplejoIdAndActiveTrue(Long tenantId, Long complejoId);

    void deleteByTenantIdAndComplejoId(Long tenantId, Long complejoId);
}
