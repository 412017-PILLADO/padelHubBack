package org.example.padelback.modules.auth.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.example.padelback.modules.auth.infrastructure.persistence.entity.UsuarioJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioJpaEntity, Long> {

    Optional<UsuarioJpaEntity> findByTenantIdAndEmailAndActiveTrue(Long tenantId, String email);

    /**
     * Búsqueda global del login. Devuelve `List` y no `Optional` a propósito: hasta que la
     * migración V20 cree el único global, dos clubes podrían compartir email y un `Optional`
     * lanzaría `NonUniqueResultException`.
     */
    List<UsuarioJpaEntity> findByEmailAndActiveTrue(String email);
}
