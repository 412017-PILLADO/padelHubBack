package org.example.padelback.infrastructure.web;

import java.time.Duration;
import java.time.Instant;

import org.example.padelback.infrastructure.config.AntiAbuseProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicWriteThrottleTest {

    private static AntiAbuseProperties props(int max, Duration ventana) {
        return new AntiAbuseProperties(2, false,
                new AntiAbuseProperties.Ip(3, Duration.ofHours(1)),
                new AntiAbuseProperties.Publico(max, ventana));
    }

    @Test
    void permiteHastaElLimiteYDespuesCorta() {
        PublicWriteThrottle t = new PublicWriteThrottle(props(3, Duration.ofMinutes(10)));
        Instant ahora = Instant.parse("2026-07-26T12:00:00Z");
        assertTrue(t.permitir("1.2.3.4", ahora));
        assertTrue(t.permitir("1.2.3.4", ahora));
        assertTrue(t.permitir("1.2.3.4", ahora));
        assertFalse(t.permitir("1.2.3.4", ahora)); // 4to en la misma ventana
        assertTrue(t.permitir("5.6.7.8", ahora));  // otra IP no se ve afectada
    }

    @Test
    void laVentanaVencidaReinicia() {
        PublicWriteThrottle t = new PublicWriteThrottle(props(1, Duration.ofMinutes(10)));
        Instant ahora = Instant.parse("2026-07-26T12:00:00Z");
        assertTrue(t.permitir("1.2.3.4", ahora));
        assertFalse(t.permitir("1.2.3.4", ahora.plusSeconds(60)));
        assertTrue(t.permitir("1.2.3.4", ahora.plus(Duration.ofMinutes(11))));
    }
}
