package org.example.padelback.modules.auth.infrastructure.persistence.adapter;

import java.util.Optional;

import org.example.padelback.modules.auth.domain.model.UsuarioAuth;
import org.example.padelback.modules.auth.domain.port.UsuarioRepositoryPort;
import org.example.padelback.modules.auth.infrastructure.persistence.repository.UsuarioJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class UsuarioRepositoryAdapter implements UsuarioRepositoryPort {

    private final UsuarioJpaRepository repo;

    @Override
    @Transactional(readOnly = true)
    public Optional<UsuarioAuth> buscarParaLogin(Long tenantId, String email) {
        return repo.findByTenantIdAndEmailAndActiveTrue(tenantId, email)
                .map(u -> new UsuarioAuth(u.getId(), u.getEmail(), u.getPasswordHash(), u.getRol()));
    }
}
