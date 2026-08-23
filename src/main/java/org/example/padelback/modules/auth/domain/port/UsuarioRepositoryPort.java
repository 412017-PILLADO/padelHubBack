package org.example.padelback.modules.auth.domain.port;

import java.util.Optional;

import org.example.padelback.modules.auth.domain.model.UsuarioAuth;

public interface UsuarioRepositoryPort {

    /** El email identifica al usuario en TODA la plataforma, no dentro de un club. */
    Optional<UsuarioAuth> buscarParaLogin(String email);
}
