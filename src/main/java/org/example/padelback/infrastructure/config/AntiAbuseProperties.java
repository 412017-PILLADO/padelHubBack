package org.example.padelback.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("padel.antiabuse")
public record AntiAbuseProperties(int maxTurnosPorTelefono, boolean trustProxy, Ip ip, Publico publico) {
    // trustProxy (padel.antiabuse.trust-proxy, default false): activarlo SOLO detrás de un proxy/CDN
    // de confianza que reescriba X-Forwarded-For. Sin proxy, el header lo pone el cliente y es
    // trivial de spoofear para evadir el límite por IP; por eso el default es false.
    public record Ip(int maxPorVentana, Duration ventana) {}

    /** Límite de escrituras públicas anónimas (arrepentimiento, link de seña) por IP por ventana. */
    public record Publico(int maxPorVentana, Duration ventana) {}
}
