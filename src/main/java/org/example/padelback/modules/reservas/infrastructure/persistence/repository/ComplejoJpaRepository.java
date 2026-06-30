package org.example.padelback.modules.reservas.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.example.padelback.modules.reservas.domain.model.ComplejoEstado;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.ComplejoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplejoJpaRepository extends JpaRepository<ComplejoJpaEntity, Long> {

    Optional<ComplejoJpaEntity> findByTenantIdAndIdAndActiveTrue(Long tenantId, Long id);

    List<ComplejoJpaEntity> findByTenantIdAndEstadoAndActiveTrue(Long tenantId, ComplejoEstado estado);
}
