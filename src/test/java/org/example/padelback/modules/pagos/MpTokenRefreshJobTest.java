package org.example.padelback.modules.pagos;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.example.padelback.modules.pagos.domain.model.CredencialMp;
import org.example.padelback.modules.pagos.domain.model.PagoMp;
import org.example.padelback.modules.pagos.domain.model.PreferenciaSena;
import org.example.padelback.modules.pagos.domain.model.TokensMp;
import org.example.padelback.modules.pagos.domain.port.CredencialMpStorePort;
import org.example.padelback.modules.pagos.domain.port.MercadoPagoGatewayPort;
import org.example.padelback.modules.pagos.infrastructure.scheduling.MpTokenRefreshJob;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MpTokenRefreshJobTest {

    private static final Instant AHORA = Instant.parse("2026-07-25T12:00:00Z");
    private final Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);

    static class StoreStub implements CredencialMpStorePort {
        List<CredencialMp> porVencer = new ArrayList<>();
        List<CredencialMp> guardadas = new ArrayList<>();

        public Optional<CredencialMp> cargar(long tenantId) { return Optional.empty(); }
        public void guardar(CredencialMp c, String pk, String scope) { guardadas.add(c); }
        public void eliminar(long tenantId) { }
        public List<CredencialMp> conVencimientoAntesDe(Instant limite) { return porVencer; }
    }

    static class GatewayStub implements MercadoPagoGatewayPort {
        boolean fallar = false;
        public TokensMp intercambiarCode(String code) { throw new UnsupportedOperationException(); }
        public TokensMp refrescar(String refreshToken) {
            if (fallar) throw new RuntimeException("mp caído");
            return new TokensMp("nuevo-token", "nuevo-refresh", "999", "PUB", "s", 15552000L);
        }
        public PreferenciaSena crearPreferencia(String a, String t, BigDecimal m, String e, String n,
                LocalDateTime x, String b) { throw new UnsupportedOperationException(); }
        public PagoMp consultarPago(String a, String p) { throw new UnsupportedOperationException(); }
    }

    @Test
    void refrescaLasCredencialesPorVencer() {
        StoreStub store = new StoreStub();
        GatewayStub gateway = new GatewayStub();
        store.porVencer.add(new CredencialMp(1L, "999", "viejo", "refresh-1",
                AHORA.plus(10, ChronoUnit.DAYS)));

        new MpTokenRefreshJob(store, gateway, clock).refrescarVencimientosProximos();

        assertEquals(1, store.guardadas.size());
        assertEquals("nuevo-token", store.guardadas.get(0).accessToken());
        assertEquals(AHORA.plusSeconds(15552000L), store.guardadas.get(0).expiraEn());
    }

    @Test
    void unaFallaNoFrenaLasDemas() {
        StoreStub store = new StoreStub();
        GatewayStub gateway = new GatewayStub();
        gateway.fallar = true;
        store.porVencer.add(new CredencialMp(1L, "1", "a", "r1", AHORA.plus(5, ChronoUnit.DAYS)));

        new MpTokenRefreshJob(store, gateway, clock).refrescarVencimientosProximos(); // no lanza

        assertEquals(0, store.guardadas.size());
    }
}
