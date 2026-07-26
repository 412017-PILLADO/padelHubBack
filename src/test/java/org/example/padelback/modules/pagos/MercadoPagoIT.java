package org.example.padelback.modules.pagos;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.example.padelback.modules.pagos.domain.model.CredencialMp;
import org.example.padelback.modules.pagos.infrastructure.persistence.TenantMercadoPagoStore;
import org.example.padelback.modules.pagos.infrastructure.persistence.SenaPagoStore;
import org.example.padelback.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

class MercadoPagoIT extends IntegrationTestBase {

    private static final long TENANT_DEMO = 1L;

    @Autowired
    TenantMercadoPagoStore credencialStore;
    @Autowired
    SenaPagoStore senaPagoStore;
    @Autowired
    JdbcTemplate jdbc;

    // ---- Stub del gateway: los IT no pegan a MP de verdad ----
    @org.springframework.boot.test.context.TestConfiguration
    static class StubMpConfig {
        static final java.util.concurrent.atomic.AtomicReference<org.example.padelback.modules.pagos.domain.model.PagoMp> PAGO =
                new java.util.concurrent.atomic.AtomicReference<>();

        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        org.example.padelback.modules.pagos.domain.port.MercadoPagoGatewayPort gatewayStub() {
            return new org.example.padelback.modules.pagos.domain.port.MercadoPagoGatewayPort() {
                @Override
                public org.example.padelback.modules.pagos.domain.model.TokensMp intercambiarCode(String code) {
                    return new org.example.padelback.modules.pagos.domain.model.TokensMp(
                            "APP_USR-stub-" + code, "TG-refresh-stub", "999", "PUB-stub",
                            "offline_access read write", 15552000L);
                }

                @Override
                public org.example.padelback.modules.pagos.domain.model.TokensMp refrescar(String refreshToken) {
                    return new org.example.padelback.modules.pagos.domain.model.TokensMp(
                            "APP_USR-refrescado", "TG-refresh-2", "999", "PUB-stub",
                            "offline_access read write", 15552000L);
                }

                @Override
                public org.example.padelback.modules.pagos.domain.model.PreferenciaSena crearPreferencia(
                        String accessToken, String titulo, java.math.BigDecimal monto, String externalReference,
                        String notificationUrl, java.time.LocalDateTime expiraEn, String backUrl) {
                    return new org.example.padelback.modules.pagos.domain.model.PreferenciaSena(
                            "pref-" + externalReference, "https://mp.stub/checkout/" + externalReference);
                }

                @Override
                public org.example.padelback.modules.pagos.domain.model.PagoMp consultarPago(
                        String accessToken, String paymentId) {
                    return PAGO.get();
                }
            };
        }
    }

    @Test
    void credencialSeGuardaCifradaYSeLeeEnClaro() {
        Instant expira = Instant.now().plus(180, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        credencialStore.guardar(new CredencialMp(TENANT_DEMO, "111", "APP_USR-token", "TG-refresh", expira),
                "pub-key", "offline_access read write");

        CredencialMp leida = credencialStore.cargar(TENANT_DEMO).orElseThrow();
        assertEquals("APP_USR-token", leida.accessToken());
        assertEquals("TG-refresh", leida.refreshToken());
        assertEquals("111", leida.mpUserId());

        // En la base tiene que estar CIFRADO, no en claro.
        String enBase = jdbc.queryForObject(
                "SELECT access_token_cif FROM tenant_mercadopago WHERE tenant_id = ?",
                String.class, TENANT_DEMO);
        assertNotEquals("APP_USR-token", enBase);

        credencialStore.eliminar(TENANT_DEMO);
        assertTrue(credencialStore.cargar(TENANT_DEMO).isEmpty());
    }

    @Test
    void senaPagoRoundTripEIdempotencia() {
        // reserva sintética directa en DB (solo para el store; el flujo real se prueba en tests posteriores)
        jdbc.update("INSERT INTO reservas (tenant_id, complejo_id, cancha_id, cliente_nombre, inicio, fin, "
                + "duracion_minutos, estado, expira_en, active, created_at, updated_at, created_by, updated_by, version) "
                + "SELECT 1, c.complejo_id, c.id, 'StoreTest', '2099-01-01 10:00:00', '2099-01-01 11:30:00', "
                + "90, 'PENDIENTE', '2099-01-01 09:00:00', 1, NOW(6), NOW(6), 'test', 'test', 0 FROM canchas c WHERE c.tenant_id = 1 LIMIT 1");
        Long reservaId = jdbc.queryForObject(
                "SELECT id FROM reservas WHERE cliente_nombre = 'StoreTest'", Long.class);

        try {
            var datos = senaPagoStore.datosParaLink(TENANT_DEMO, reservaId).orElseThrow();
            assertEquals("PENDIENTE", datos.estadoReserva());

            senaPagoStore.guardarPreferencia(TENANT_DEMO, reservaId, "pref-1", "https://mp/init/1",
                    new BigDecimal("5000.00"));
            assertEquals("https://mp/init/1",
                    senaPagoStore.cargarPorReserva(TENANT_DEMO, reservaId).orElseThrow().initPoint());

            assertFalse(senaPagoStore.pagoYaProcesado("pay-9"));
            senaPagoStore.registrarPago(TENANT_DEMO, reservaId, "pay-9", "APROBADO");
            assertTrue(senaPagoStore.pagoYaProcesado("pay-9"));
        } finally {
            // La DB de Testcontainers es compartida entre todas las clases IT (ver javadoc de
            // IntegrationTestBase): sin este cleanup, la reserva sintética PENDIENTE con
            // expira_en en 2099 queda "vigente" y puede filtrarse a tests posteriores que listen
            // pendientes de seña.
            jdbc.update("DELETE FROM sena_pagos WHERE reserva_id = ?", reservaId);
            jdbc.update("DELETE FROM reservas WHERE id = ?", reservaId);
        }
    }

    @Test
    void flujoDeConexionOAuthCompleto() {
        // 1. el panel pide la URL de autorización
        HttpHeaders auth = ownerHeaders(); // Bearer del owner + X-Tenant demo (IntegrationTestBase)
        ResponseEntity<Map> conectar = exchange(HttpMethod.POST, "/api/v1/pagos/mp/conectar",
                Map.of("returnTo", "http://demo.localhost:4400/admin/config"), auth, Map.class);
        assertEquals(200, conectar.getStatusCode().value());
        String url = (String) conectar.getBody().get("url");
        assertTrue(url.contains("client_id="));
        String state = url.substring(url.indexOf("state=") + 6).split("&")[0];

        // 2. MP redirige al callback público con code + state (gateway stubbeado)
        ResponseEntity<Void> callback = exchange(HttpMethod.GET,
                "/public/pagos/mp/oauth/callback?code=TG-test&state=" + state, null, publicHeaders(), Void.class);
        assertEquals(302, callback.getStatusCode().value());
        assertTrue(callback.getHeaders().getLocation().toString()
                .startsWith("http://demo.localhost:4400/admin/config"));

        // 3. el estado del panel refleja la conexión
        ResponseEntity<Map> estado = exchange(HttpMethod.GET, "/api/v1/pagos/mp/estado", null, auth, Map.class);
        assertEquals(Boolean.TRUE, estado.getBody().get("conectado"));
        assertEquals("999", estado.getBody().get("mpUserId"));

        // 4. desconectar limpia
        assertEquals(204, exchange(HttpMethod.POST, "/api/v1/pagos/mp/desconectar", null, auth, Void.class)
                .getStatusCode().value());
        assertEquals(Boolean.FALSE, exchange(HttpMethod.GET, "/api/v1/pagos/mp/estado", null, auth, Map.class)
                .getBody().get("conectado"));
    }

    @Test
    void callbackConStateAdulteradoNoConecta() {
        ResponseEntity<Void> callback = exchange(HttpMethod.GET,
                "/public/pagos/mp/oauth/callback?code=TG-x&state=chorizo.invalido", null, publicHeaders(), Void.class);
        assertEquals(400, callback.getStatusCode().value());
    }

    @Test
    void configPublicaExponePagoOnlineSegunConexion() {
        // sin credencial → false
        credencialStore.eliminar(TENANT_DEMO);
        ResponseEntity<Map> antes = exchange(HttpMethod.GET, "/public/config", null, publicHeaders(), Map.class);
        assertEquals(Boolean.FALSE, antes.getBody().get("pagoOnline"));

        // con credencial vigente → true
        credencialStore.guardar(new CredencialMp(TENANT_DEMO, "999", "tok", "ref",
                Instant.now().plus(90, ChronoUnit.DAYS)), "PUB", "s");
        ResponseEntity<Map> despues = exchange(HttpMethod.GET, "/public/config", null, publicHeaders(), Map.class);
        assertEquals(Boolean.TRUE, despues.getBody().get("pagoOnline"));

        credencialStore.eliminar(TENANT_DEMO); // limpiar para otros tests
    }
}
