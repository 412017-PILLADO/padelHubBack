package org.example.padelback.modules.tenant.infrastructure.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autenticación por clave de plataforma como <b>fallback para scripts</b>: si el request trae
 * {@code X-Platform-Key} igual a la clave configurada, se le concede rol SUPERADMIN para esa request
 * (sin JWT). El camino normal (panel de dev) usa el JWT de super-admin; este filtro solo cubre el
 * uso por curl/CI. Si no hay clave configurada, el filtro no hace nada.
 */
public class PlatformKeyAuthFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Platform-Key";

    private final String adminKey;

    public PlatformKeyAuthFilter(String adminKey) {
        this.adminKey = adminKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (adminKey != null && !adminKey.isBlank()
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String key = request.getHeader(HEADER);
            if (key != null && constantTimeEquals(adminKey, key)) {
                var auth = new PlatformKeyAuthentication();
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }

    /** Token mínimo autenticado con autoridad SUPERADMIN (sin principal de usuario). */
    private static final class PlatformKeyAuthentication extends AbstractAuthenticationToken {
        PlatformKeyAuthentication() {
            super(List.of(new SimpleGrantedAuthority("ROLE_SUPERADMIN")));
            setAuthenticated(true);
        }
        @Override public Object getCredentials() { return ""; }
        @Override public Object getPrincipal() { return "platform-key"; }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
