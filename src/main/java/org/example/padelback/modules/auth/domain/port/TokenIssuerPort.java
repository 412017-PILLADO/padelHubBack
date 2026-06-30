package org.example.padelback.modules.auth.domain.port;

import org.example.padelback.modules.auth.domain.model.UsuarioRol;

public interface TokenIssuerPort {
    String emitir(Long tenantId, String email, UsuarioRol rol);
    long expiracionMs();
}
