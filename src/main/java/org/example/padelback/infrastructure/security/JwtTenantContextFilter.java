package org.example.padelback.infrastructure.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.example.padelback.infrastructure.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JwtTenantContextFilter extends OncePerRequestFilter {

    private final JwtTenantResolver jwtTenantResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            jwtTenantResolver.resolve(SecurityContextHolder.getContext().getAuthentication())
                    .ifPresent(TenantContext::setTenantId);
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
