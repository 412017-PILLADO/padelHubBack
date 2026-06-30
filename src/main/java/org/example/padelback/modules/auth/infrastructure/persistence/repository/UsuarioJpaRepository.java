package org.example.padelback.modules.auth.infrastructure.persistence.repository;

import java.util.Optional;

import org.example.padelback.modules.auth.infrastructure.persistence.entity.UsuarioJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioJpaEntity, Long> {
    Optional<UsuarioJpaEntity> findByTenantIdAndEmailAndActiveTrue(Long tenantId, String email);
}
