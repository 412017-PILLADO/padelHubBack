package org.example.padelback.modules.auth.presentation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.example.padelback.domain.exception.TenantNotResolvedException;
import org.example.padelback.infrastructure.tenancy.PublicTenantContextFilter;
import org.example.padelback.modules.auth.application.CanjearCodigoUseCase;
import org.example.padelback.modules.auth.application.LoginUseCase;
import org.example.padelback.modules.auth.domain.exception.CredencialesInvalidasException;
import org.example.padelback.modules.auth.infrastructure.security.LoginThrottle;
import org.example.padelback.modules.auth.presentation.dto.CanjeRequest;
import org.example.padelback.modules.auth.presentation.dto.CanjeResponse;
import org.example.padelback.modules.auth.presentation.dto.LoginRequest;
import org.example.padelback.modules.auth.presentation.dto.LoginResponse;
import org.example.padelback.modules.auth.presentation.dto.MeResponse;
import org.example.padelback.modules.tenant.infrastructure.PublicTenantResolver;
import org.example.padelback.modules.tenant.infrastructure.persistence.repository.TenantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final CanjearCodigoUseCase canjearCodigoUseCase;
    private final PublicTenantResolver tenantResolver;
    private final TenantJpaRepository tenantRepository;
    private final LoginThrottle throttle;

    /**
     * Paso 1, en el apex: NO lee {@code X-Tenant} ni el host — el club sale del email. Devuelve a
     * qué subdominio redirigir y con qué código entrar.
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        String ip = http.getRemoteAddr();
        throttle.assertNotLocked(req.email(), ip);
        try {
            var result = loginUseCase.ejecutar(req.email(), req.password());
            throttle.recordSuccess(req.email());
            return new LoginResponse(result.slug(), result.code());
        } catch (CredencialesInvalidasException ex) {
            throttle.recordFailure(req.email(), ip);
            throw ex;
        }
    }

    /**
     * Paso 2, ya en el subdominio del club: cambia el código por el JWT. Acá el {@code X-Tenant} SÍ
     * importa — es lo que se compara contra el club del código.
     */
    @PostMapping("/canjear")
    public CanjeResponse canjear(@Valid @RequestBody CanjeRequest req, HttpServletRequest http) {
        Long tenantId = tenantResolver
                .resolve(http.getHeader(PublicTenantContextFilter.TENANT_HEADER), http.getServerName())
                .orElseThrow(() -> new TenantNotResolvedException("Tenant no resuelto para el host"));
        var result = canjearCodigoUseCase.ejecutar(req.code(), tenantId);
        return new CanjeResponse(result.token(), result.expiresIn());
    }

    @GetMapping("/me")
    public MeResponse me(JwtAuthenticationToken auth) {
        Jwt jwt = auth.getToken();
        Object tenantClaim = jwt.getClaim("tenantId");
        Long tenantId = tenantClaim instanceof Number n ? n.longValue() : Long.valueOf(String.valueOf(tenantClaim));
        var roles = jwt.getClaimAsStringList("roles");
        String tenantName = tenantRepository.findById(tenantId)
                .map(t -> t.getName())
                .orElse(null);
        return new MeResponse(jwt.getSubject(), tenantId, tenantName, roles == null || roles.isEmpty() ? null : roles.get(0));
    }
}
