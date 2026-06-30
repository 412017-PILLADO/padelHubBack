package org.example.padelback.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("padel.antiabuse")
public record AntiAbuseProperties(int maxTurnosPorTelefono, Ip ip) {
    public record Ip(int maxPorVentana, Duration ventana) {}
}
