package org.example.padelback.infrastructure.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtTenantResolver {

    public static final String TENANT_CLAIM = "tenantId";

    public Optional<Long> resolve(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwt) || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return parse(jwt.getToken().getClaim(TENANT_CLAIM));
    }

    private Optional<Long> parse(Object claim) {
        if (claim instanceof Number n) {
            return Optional.of(n.longValue());
        }
        if (claim instanceof String s && StringUtils.hasText(s)) {
            try {
                return Optional.of(Long.parseLong(s));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
