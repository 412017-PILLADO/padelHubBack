package org.example.padelback.modules.reservas.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.example.padelback.modules.reservas.infrastructure.persistence.entity.ArrepentimientoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArrepentimientoJpaRepository extends JpaRepository<ArrepentimientoJpaEntity, Long> {

    List<ArrepentimientoJpaEntity> findByTenantIdAndActiveTrueOrderByGestionadoAscCreatedAtDesc(Long tenantId);

    Optional<ArrepentimientoJpaEntity> findByTenantIdAndIdAndActiveTrue(Long tenantId, Long id);
}
