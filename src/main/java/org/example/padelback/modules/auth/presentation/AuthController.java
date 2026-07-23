package org.example.padelback.modules.auth.presentation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.example.padelback.domain.exception.TenantNotResolvedException;
import org.example.padelback.modules.auth.application.LoginUseCase;
import org.example.padelback.modules.auth.domain.exception.CredencialesInvalidasException;
import org.example.padelback.modules.auth.infrastructure.security.LoginThrottle;
import org.example.padelback.modules.auth.presentation.dto.LoginRequest;
import org.example.padelback.modules.auth.presentation.dto.LoginResponse;
import org.example.padelback.modules.auth.presentation.dto.MeResponse;
import org.example.padelback.infrastructure.tenancy.PublicTenantContextFilter;
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
    private final PublicTenantResolver tenantResolver;
    private final TenantJpaRepository tenantRepository;
    private final LoginThrottle throttle;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        Long tenantId = tenantResolver
                .resolve(http.getHeader(PublicTenantContextFilter.TENANT_HEADER), http.getServerName())
                .orElseThrow(() -> new TenantNotResolvedException("Tenant no resuelto para el host"));
        String ip = http.getRemoteAddr();
        throttle.assertNotLocked(tenantId, req.email(), ip);
        try {
            var result = loginUseCase.ejecutar(tenantId, req.email(), req.password());
            throttle.recordSuccess(tenantId, req.email());
            return new LoginResponse(result.token(), result.expiresIn());
        } catch (CredencialesInvalidasException ex) {
            throttle.recordFailure(tenantId, req.email(), ip);
            throw ex;
        }
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
