package org.example.padelback.infrastructure.config;

import java.time.Clock;
import java.time.ZoneId;

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

    /** Complejo de pádel: Córdoba, Argentina (UTC-3). */
    public static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Argentina/Cordoba");

    @Bean
    public Clock clock() {
        return Clock.system(ZONA_NEGOCIO);
    }
}
