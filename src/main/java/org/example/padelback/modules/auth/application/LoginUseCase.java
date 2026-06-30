package org.example.padelback.modules.auth.application;

import org.example.padelback.modules.auth.domain.exception.CredencialesInvalidasException;
import org.example.padelback.modules.auth.domain.model.UsuarioAuth;
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

    public record LoginResult(String token, long expiresIn) {}

    public LoginResult ejecutar(Long tenantId, String email, String password) {
        UsuarioAuth usuario = usuarioRepo.buscarParaLogin(tenantId, email)
                .orElseThrow(CredencialesInvalidasException::new);
        if (!passwordEncoder.matches(password, usuario.passwordHash())) {
            throw new CredencialesInvalidasException();
        }
        String token = tokenIssuer.emitir(tenantId, usuario.email(), usuario.rol());
        return new LoginResult(token, tokenIssuer.expiracionMs());
    }
}
