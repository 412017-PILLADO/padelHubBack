package org.example.padelback.modules.auth.application;

import org.example.padelback.modules.auth.domain.exception.CredencialesInvalidasException;
import org.example.padelback.modules.auth.domain.model.UsuarioAuth;
import org.example.padelback.modules.auth.domain.port.TenantEstadoPort;
import org.example.padelback.modules.auth.domain.port.UsuarioRepositoryPort;
import org.example.padelback.modules.auth.infrastructure.security.CodigoIngresoStore;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Primer paso del login: el email dice de qué club es quien entra.
 *
 * <p>No devuelve el JWT sino el slug del club y un código de canje. El JWT tiene que terminar en el
 * {@code localStorage} del subdominio del club, y el apex no puede escribir ahí: escribe la URL a
 * la que redirige, y el código es lo que viaja en ella.
 *
 * <p>Email inexistente, contraseña incorrecta y club INACTIVE salen todos por
 * {@link CredencialesInvalidasException}. Ver el javadoc de {@code LoginUseCaseTest}.
 */
@Service
@RequiredArgsConstructor
public class LoginUseCase {

    private final UsuarioRepositoryPort usuarioRepo;
    private final PasswordEncoder passwordEncoder;
    private final TenantEstadoPort tenantEstado;
    private final CodigoIngresoStore codigos;

    public record LoginResult(String slug, String code) {}

    public LoginResult ejecutar(String email, String password) {
        if (email == null) {
            throw new CredencialesInvalidasException();
        }
        UsuarioAuth usuario = usuarioRepo.buscarParaLogin(email.trim().toLowerCase())
                .orElseThrow(CredencialesInvalidasException::new);
        if (!passwordEncoder.matches(password, usuario.passwordHash())) {
            throw new CredencialesInvalidasException();
        }
        String slug = tenantEstado.slugSiActivo(usuario.tenantId())
                .orElseThrow(CredencialesInvalidasException::new);
        return new LoginResult(slug, codigos.emitir(usuario.tenantId(), usuario.email(), usuario.rol()));
    }
}
