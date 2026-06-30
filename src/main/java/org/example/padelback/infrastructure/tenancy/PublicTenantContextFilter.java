package org.example.padelback.infrastructure.tenancy;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.example.padelback.modules.tenant.infrastructure.PublicTenantResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Único punto de resolución del tenant para la superficie pública. Prioriza el slug del header
 * {@code X-Tenant} (lo manda el front desde su subdominio) y cae a host si no viene; fija el
 * TenantContext. Si no resuelve, deja el contexto sin setear (las queries con requireTenantId
 * fallarán → 404 vía el handler).
 */
@RequiredArgsConstructor
public class PublicTenantContextFilter extends OncePerRequestFilter {

    /** Header con el slug del tenant que envía el front (front y back desplegados por separado). */
    public static final String TENANT_HEADER = "X-Tenant";

    private final PublicTenantResolver resolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            resolver.resolve(request.getHeader(TENANT_HEADER), request.getServerName())
                    .ifPresent(TenantContext::setTenantId);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
