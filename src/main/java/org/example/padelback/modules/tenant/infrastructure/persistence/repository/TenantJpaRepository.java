package org.example.padelback.modules.tenant.infrastructure.persistence.repository;

import java.util.Optional;

import org.example.padelback.modules.tenant.infrastructure.persistence.entity.TenantJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantJpaRepository extends JpaRepository<TenantJpaEntity, Long> {

    Optional<TenantJpaEntity> findBySlug(String slug);
}
