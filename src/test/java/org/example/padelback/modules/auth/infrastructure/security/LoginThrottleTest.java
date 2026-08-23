package org.example.padelback.modules.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/**
 * El freno pasa a contar por email solo: en el apex, cuando hay que frenar, todavía no se sabe de
 * qué club es quien está probando. Con el único global sobre `usuarios.email`, la key por email es
 * igual de precisa que la vieja (tenant+email).
 */
class LoginThrottleTest {

    private final LoginThrottle throttle = new LoginThrottle();

    @Test
    void alLlegarAlTopeDeFallos_laCuentaQuedaBloqueada() {
        for (int i = 0; i < LoginThrottle.MAX_FAILS_CREDENCIAL; i++) {
            throttle.recordFailure("owner@club.com", "1.1.1.1");
        }

        assertThatThrownBy(() -> throttle.assertNotLocked("owner@club.com", "1.1.1.1"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void unLoginExitosoLimpiaLosFallosDeEsaCuenta() {
        for (int i = 0; i < LoginThrottle.MAX_FAILS_CREDENCIAL - 1; i++) {
            throttle.recordFailure("owner@club.com", "1.1.1.1");
        }
        throttle.recordSuccess("owner@club.com");
        throttle.recordFailure("owner@club.com", "1.1.1.1");

        assertThatCode(() -> throttle.assertNotLocked("owner@club.com", "1.1.1.1"))
                .doesNotThrowAnyException();
    }

    @Test
    void muchosFallosDesdeLaMismaIpConCuentasDistintas_bloqueanLaIp() {
        // El apex hace que probar emails de muchos clubes salga por el mismo endpoint: el freno
        // por IP es lo que impide barrer cuentas sin llegar nunca al tope de ninguna.
        for (int i = 0; i < LoginThrottle.MAX_FAILS_IP; i++) {
            throttle.recordFailure("owner" + i + "@club.com", "9.9.9.9");
        }

        assertThatThrownBy(() -> throttle.assertNotLocked("recien-llegado@club.com", "9.9.9.9"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void otraIpNoQuedaAfectada() {
        for (int i = 0; i < LoginThrottle.MAX_FAILS_IP; i++) {
            throttle.recordFailure("owner" + i + "@club.com", "9.9.9.9");
        }

        assertThatCode(() -> throttle.assertNotLocked("recien-llegado@club.com", "8.8.8.8"))
                .doesNotThrowAnyException();
    }
}
