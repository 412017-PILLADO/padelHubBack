package org.example.padelback.modules.reservas.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/** Resuelve la IP del cliente detrás del proxy: 1er IP de X-Forwarded-For, o RemoteAddr. */
@Component
public class ClientIpResolver {
    public String resolve(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
