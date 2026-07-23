package org.example.padelback.modules.tenant.presentation;

import org.example.padelback.modules.auth.domain.model.UsuarioRol;
import org.example.padelback.modules.auth.domain.port.TokenIssuerPort;
import org.example.padelback.modules.auth.presentation.dto.LoginResponse;
import org.example.padelback.modules.tenant.infrastructure.persistence.PlatformAdminStore;
import org.example.padelback.modules.tenant.infrastructure.security.PlatformLoginThrottle;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Login del super-admin de plataforma. No usa {@code X-Tenant} (el super-admin no pertenece a un
 * tenant): valida contra {@code platform_admins} y emite un JWT con rol SUPERADMIN y sin claim de
 * tenant. Endpoint público (permitAll); el resto de {@code /platform/**} requiere SUPERADMIN.
 */
@RestController
@RequestMapping("/platform/auth")
@RequiredArgsConstructor
public class PlatformAuthController {

    private final PlatformAdminStore admins;
    private final PasswordEncoder passwordEncoder;
    private final TokenIssuerPort tokenIssuer;
    private final PlatformLoginThrottle throttle;

    public record PlatformLoginRequest(@NotBlank String email, @NotBlank String password) {}

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody PlatformLoginRequest req, HttpServletRequest http) {
        String email = req.email().trim().toLowerCase();
        String ip = http.getRemoteAddr();
        throttle.assertNotLocked(email, ip);
        var admin = admins.findActivoByEmail(email)
                .filter(a -> passwordEncoder.matches(req.password(), a.passwordHash()))
                .orElse(null);
        if (admin == null) {
            throttle.recordFailure(email, ip);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas.");
        }
        throttle.recordSuccess(email, ip);
        String token = tokenIssuer.emitir(null, admin.email(), UsuarioRol.SUPERADMIN);
        return new LoginResponse(token, tokenIssuer.expiracionMs());
    }
}
