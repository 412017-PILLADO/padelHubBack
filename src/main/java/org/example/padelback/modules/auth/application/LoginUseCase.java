package org.example.padelback.modules.auth.application;

import org.example.padelback.modules.auth.domain.exception.CredencialesInvalidasException;
import org.example.padelback.modules.auth.domain.model.UsuarioAuth;
import org.example.padelback.modules.auth.domain.port.TenantEstadoPort;
import org.example.padelback.modules.auth.domain.port.TokenIssuerPort;
import org.example.padelback.modules.auth.domain.port.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginUseCase {

    private final UsuarioRepositoryPort usuarioRepo;
    private final TokenIssuerPort tokenIssuer;
    private final PasswordEncoder passwordEncoder;
    private final TenantEstadoPort tenantEstado;

    public record LoginResult(String token, long expiresIn) {}

    public LoginResult ejecutar(Long tenantId, String email, String password) {
        // Tenant INACTIVE: cortamos antes de tocar usuarios, con el mismo mensaje genérico de
        // credenciales inválidas (no filtramos el estado del tenant al cliente).
        if (!tenantEstado.estaActivo(tenantId)) {
            throw new CredencialesInvalidasException();
        }
        UsuarioAuth usuario = usuarioRepo.buscarParaLogin(tenantId, email)
                .orElseThrow(CredencialesInvalidasException::new);
        if (!passwordEncoder.matches(password, usuario.passwordHash())) {
            throw new CredencialesInvalidasException();
        }
        String token = tokenIssuer.emitir(tenantId, usuario.email(), usuario.rol());
        return new LoginResult(token, tokenIssuer.expiracionMs());
    }
}
