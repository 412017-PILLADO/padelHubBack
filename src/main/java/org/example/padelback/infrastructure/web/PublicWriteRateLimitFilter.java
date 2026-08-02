package org.example.padelback.infrastructure.web;

import java.io.IOException;
import java.time.Clock;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.example.padelback.modules.reservas.infrastructure.web.ClientIpResolver;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 429 para POSTs públicos anónimos que exceden la ventana. Se registra SOLO sobre
 * /public/arrepentimiento y /public/pagos/mp/preferencia (ver PublicTenancyConfig): el webhook y el
 * callback OAuth quedan afuera a propósito (tráfico de MP; un 429 podría perder notificaciones).
 */
public class PublicWriteRateLimitFilter extends OncePerRequestFilter {

    private final PublicWriteThrottle throttle;
    private final ClientIpResolver ipResolver;
    private final Clock clock;

    public PublicWriteRateLimitFilter(PublicWriteThrottle throttle, ClientIpResolver ipResolver, Clock clock) {
        this.throttle = throttle;
        this.ipResolver = ipResolver;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if ("POST".equalsIgnoreCase(request.getMethod())
                && !throttle.permitir(ipResolver.resolve(request), clock.instant())) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"error\":\"Demasiadas solicitudes desde este dispositivo. Probá más tarde.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
