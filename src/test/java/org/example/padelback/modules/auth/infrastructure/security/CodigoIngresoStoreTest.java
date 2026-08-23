package org.example.padelback.modules.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.example.padelback.modules.auth.domain.model.UsuarioRol;
import org.junit.jupiter.api.Test;

/**
 * El código de canje es lo único que separa "me autentiqué en el apex" de "tengo sesión en el
 * subdominio de mi club". Sus dos garantías —vive poco y sirve una sola vez— son las que hacen que
 * pasarlo por la barra de direcciones sea aceptable, así que se prueban acá y no de casualidad.
 */
class CodigoIngresoStoreTest {

    /** Reloj que avanza a mano: el TTL se verifica sin dormir el test. */
    private static final class RelojDeMentira extends Clock {
        private Instant ahora = Instant.parse("2026-08-23T10:00:00Z");

        void avanzar(Duration d) {
            ahora = ahora.plus(d);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    private final RelojDeMentira reloj = new RelojDeMentira();
    private final CodigoIngresoStore store = new CodigoIngresoStore(reloj);

    @Test
    void codigoRecienEmitido_devuelveLoQueSeGuardo() {
        String codigo = store.emitir(7L, "owner@club.com", UsuarioRol.OWNER);

        assertThat(store.consumir(codigo))
                .contains(new CodigoIngresoStore.Entrada(7L, "owner@club.com", UsuarioRol.OWNER));
    }

    @Test
    void elSegundoCanjeDelMismoCodigo_noDevuelveNada() {
        String codigo = store.emitir(7L, "owner@club.com", UsuarioRol.OWNER);
        store.consumir(codigo);

        assertThat(store.consumir(codigo)).isEmpty();
    }

    @Test
    void codigoVencido_noSeCanjea() {
        String codigo = store.emitir(7L, "owner@club.com", UsuarioRol.OWNER);
        reloj.avanzar(CodigoIngresoStore.TTL.plusSeconds(1));

        assertThat(store.consumir(codigo)).isEmpty();
    }

    @Test
    void codigoJustoAntesDelVencimiento_todaviaSirve() {
        String codigo = store.emitir(7L, "owner@club.com", UsuarioRol.OWNER);
        reloj.avanzar(CodigoIngresoStore.TTL.minusSeconds(1));

        assertThat(store.consumir(codigo)).isPresent();
    }

    @Test
    void codigoInexistenteONulo_noDevuelveNada() {
        assertThat(store.consumir("no-existe")).isEmpty();
        assertThat(store.consumir(null)).isEmpty();
        assertThat(store.consumir("  ")).isEmpty();
    }

    @Test
    void dosEmisiones_nuncaDanElMismoCodigo() {
        String uno = store.emitir(7L, "owner@club.com", UsuarioRol.OWNER);
        String otro = store.emitir(7L, "owner@club.com", UsuarioRol.OWNER);

        assertThat(uno).isNotEqualTo(otro);
    }
}
