package org.example.padelback.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import org.example.padelback.infrastructure.persistence.entity.BaseJpaEntity;

@NoRepositoryBean
public interface BaseJpaRepository<T extends BaseJpaEntity> extends JpaRepository<T, Long>, JpaSpecificationExecutor<T> {

    List<T> findAllByTenantIdAndActiveTrue(Long tenantId);

    Optional<T> findByIdAndTenantIdAndActiveTrue(Long id, Long tenantId);

    boolean existsByIdAndTenantIdAndActiveTrue(Long id, Long tenantId);
}
