package org.example.padelback.modules.auth.domain.port;

import java.util.Optional;

import org.example.padelback.modules.auth.domain.model.UsuarioAuth;

public interface UsuarioRepositoryPort {
    Optional<UsuarioAuth> buscarParaLogin(Long tenantId, String email);
}
