package org.example.padelback.modules.pagos;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import org.example.padelback.infrastructure.config.CryptoProperties;
import org.example.padelback.modules.pagos.domain.exception.MpCallbackInvalidoException;
import org.example.padelback.modules.pagos.infrastructure.crypto.MpOAuthStateCodec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MpOAuthStateCodecTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final CryptoProperties PROPS = new CryptoProperties(KEY);

    @Test
    void rondaCompletaDevuelveTenantYReturnTo() {
        MpOAuthStateCodec codec = new MpOAuthStateCodec(PROPS, Clock.systemUTC());
        String state = codec.crear(7L, "http://demo.localhost:4400/admin/config");
        MpOAuthStateCodec.StateData data = codec.validar(state);
        assertEquals(7L, data.tenantId());
        assertEquals("http://demo.localhost:4400/admin/config", data.returnTo());
    }

    @Test
    void stateVencidoEsRechazado() {
        Clock emision = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        Clock validacion = Clock.fixed(Instant.parse("2026-01-01T00:11:00Z"), ZoneOffset.UTC); // +11 min
        String state = new MpOAuthStateCodec(PROPS, emision).crear(1L, "http://x/admin");
        MpOAuthStateCodec tardio = new MpOAuthStateCodec(PROPS, validacion);
        assertThrows(MpCallbackInvalidoException.class, () -> tardio.validar(state));
    }

    @Test
    void stateAdulteradoEsRechazado() {
        MpOAuthStateCodec codec = new MpOAuthStateCodec(PROPS, Clock.systemUTC());
        String state = codec.crear(1L, "http://x/admin");
        String roto = state.substring(0, state.length() - 2) + "zz";
        assertThrows(MpCallbackInvalidoException.class, () -> codec.validar(roto));
    }

    @Test
    void stateConBase64InvalidoEsRechazado() {
        MpOAuthStateCodec codec = new MpOAuthStateCodec(PROPS, Clock.systemUTC());
        assertThrows(MpCallbackInvalidoException.class, () -> codec.validar("abc.!!!"));
        assertThrows(MpCallbackInvalidoException.class, () -> codec.validar("!!!.abc"));
    }
}
