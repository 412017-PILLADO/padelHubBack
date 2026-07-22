package org.example.padelback.modules.tenant.infrastructure.persistence.repository;

import java.util.Optional;

import org.example.padelback.modules.tenant.domain.model.TenantStatus;
import org.example.padelback.modules.tenant.infrastructure.persistence.entity.TenantJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantJpaRepository extends JpaRepository<TenantJpaEntity, Long> {

    Optional<TenantJpaEntity> findBySlug(String slug);

    /** Usado por la resolución pública: un tenant INACTIVE no debe resolver (landing ni login). */
    Optional<TenantJpaEntity> findBySlugAndStatus(String slug, TenantStatus status);

    Optional<TenantJpaEntity> findByIdAndStatus(Long id, TenantStatus status);
}
