package org.example.padelback.modules.reservas.infrastructure.web;

import org.example.padelback.infrastructure.config.AntiAbuseProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resuelve la IP del cliente. X-Forwarded-For lo puede fijar cualquiera que pegue directo al server,
 * así que confiar en él sin un proxy de confianza adelante deja spoofear la IP y evadir el límite
 * anti-abuso por IP. Por eso solo se usa el header cuando {@code padel.antiabuse.trust-proxy=true}
 * (hay un proxy/CDN que lo reescribe); si no, se usa {@code getRemoteAddr()}, la IP real del socket.
 */
@Component
@RequiredArgsConstructor
public class ClientIpResolver {

    private final AntiAbuseProperties props;

    public String resolve(HttpServletRequest request) {
        if (props.trustProxy()) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
