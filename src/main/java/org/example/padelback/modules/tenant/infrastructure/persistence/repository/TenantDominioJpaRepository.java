package org.example.padelback.modules.tenant.infrastructure.persistence.repository;

import java.util.Optional;

import org.example.padelback.modules.tenant.infrastructure.persistence.entity.TenantDominioJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantDominioJpaRepository extends JpaRepository<TenantDominioJpaEntity, Long> {

    Optional<TenantDominioJpaEntity> findByHost(String host);
}
