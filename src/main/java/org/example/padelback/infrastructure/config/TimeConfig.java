package org.example.padelback.infrastructure.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj de negocio. El JVM corre pineado a UTC (horas determinísticas en cualquier server),
 * así que {@code LocalDateTime.now()} crudo da hora UTC. Para reglas que dependen del "ahora"
 * de pared del negocio —p. ej. no dejar reservar un horario que ya pasó— hay que pedir la hora
 * EN la zona del complejo. Inyectable como {@link Clock} para poder fijarlo en los tests.
 */
@Configuration
public class TimeConfig {

    /** Zona por defecto: Córdoba, Argentina (UTC-3). Configurable por {@code padel.zona-negocio}. */
    public static final String ZONA_NEGOCIO_DEFAULT = "America/Argentina/Cordoba";

    /** Zona del negocio, inyectada desde {@code padel.zona-negocio} (env {@code PADEL_ZONA_NEGOCIO}). */
    @Bean
    public Clock clock(@Value("${padel.zona-negocio:" + ZONA_NEGOCIO_DEFAULT + "}") String zonaNegocio) {
        return Clock.system(ZoneId.of(zonaNegocio));
    }
}
