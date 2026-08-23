package org.example.padelback.modules.auth.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.example.padelback.modules.auth.domain.model.UsuarioAuth;
import org.example.padelback.modules.auth.domain.port.UsuarioRepositoryPort;
import org.example.padelback.modules.auth.infrastructure.persistence.entity.UsuarioJpaEntity;
import org.example.padelback.modules.auth.infrastructure.persistence.repository.UsuarioJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * La consulta es global (sin tenant) y eso es seguro: {@code TenantFilterAspect} sólo activa el
 * {@code tenantFilter} de Hibernate cuando hay {@code TenantContext}, y en el login todavía no lo
 * hay — igual que antes de este cambio, cuando el tenant se pasaba a mano.
 */
@Repository
@RequiredArgsConstructor
public class UsuarioRepositoryAdapter implements UsuarioRepositoryPort {

    private final UsuarioJpaRepository repo;

    @Override
    @Transactional(readOnly = true)
    public Optional<UsuarioAuth> buscarParaLogin(String email) {
        return unico(repo.findByEmailAndActiveTrue(email))
                .map(u -> new UsuarioAuth(u.getId(), u.getTenantId(), u.getEmail(), u.getPasswordHash(), u.getRol()));
    }

    /**
     * Un email ambiguo no autentica a nadie. Mientras no exista el único global, esta regla es lo
     * que convierte un dato sucio en el 401 de siempre en vez de en un 500.
     */
    static Optional<UsuarioJpaEntity> unico(List<UsuarioJpaEntity> encontrados) {
        return encontrados.size() == 1 ? Optional.of(encontrados.get(0)) : Optional.empty();
    }
}
