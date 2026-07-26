package org.example.padelback.modules.pagos;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.example.padelback.modules.pagos.domain.model.CredencialMp;
import org.example.padelback.modules.pagos.domain.model.PagoMp;
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

    @Test
    void linkDeSenaIdempotenteYSoloParaPendientes() {
        conectarDemoStub();
        activarSena();
        try {
            long reservaId = crearReservaPendientePorApi(LocalDate.now().plusDays(33));

            ResponseEntity<Map> r1 = exchange(HttpMethod.POST, "/public/pagos/mp/preferencia",
                    Map.of("reservaId", reservaId, "backUrl", "http://demo.localhost:4400"), publicHeaders(), Map.class);
            assertEquals(200, r1.getStatusCode().value());
            String initPoint = (String) r1.getBody().get("initPoint");
            assertTrue(initPoint.startsWith("https://mp.stub/checkout/"));

            // idempotencia: la segunda llamada no crea otra preferencia, devuelve la misma.
            ResponseEntity<Map> r2 = exchange(HttpMethod.POST, "/public/pagos/mp/preferencia",
                    Map.of("reservaId", reservaId, "backUrl", "http://demo.localhost:4400"), publicHeaders(), Map.class);
            assertEquals(initPoint, r2.getBody().get("initPoint"));
        } finally {
            setSena(false, null);
            credencialStore.eliminar(TENANT_DEMO);
        }
    }

    @Test
    void sinCredencialElLinkDa409() {
        credencialStore.eliminar(TENANT_DEMO);
        activarSena();
        try {
            long reservaId = crearReservaPendientePorApi(LocalDate.now().plusDays(34));
            ResponseEntity<Map> r = exchange(HttpMethod.POST, "/public/pagos/mp/preferencia",
                    Map.of("reservaId", reservaId, "backUrl", "http://demo.localhost:4400"), publicHeaders(), Map.class);
            assertEquals(409, r.getStatusCode().value());
        } finally {
            setSena(false, null);
        }
    }

    @Test
    void webhookAprobadoConfirmaLaReservaYEsIdempotente() {
        conectarDemoStub();
        activarSena();
        try {
            long reservaId = crearReservaPendientePorApi(LocalDate.now().plusDays(35));
            exchange(HttpMethod.POST, "/public/pagos/mp/preferencia",
                    Map.of("reservaId", reservaId, "backUrl", "http://x/"), publicHeaders(), Map.class);

            StubMpConfig.PAGO.set(new PagoMp("777", "approved", TENANT_DEMO + "-" + reservaId,
                    new BigDecimal("5000.00")));

            ResponseEntity<Void> wh = exchange(HttpMethod.POST, "/public/pagos/mp/webhook?tenant=demo",
                    Map.of("type", "payment", "data", Map.of("id", "777")), publicHeaders(), Void.class);
            assertEquals(200, wh.getStatusCode().value());

            // la reserva quedó CONFIRMADO (visible en el panel de turnos)
            String estado = jdbc.queryForObject("SELECT estado FROM reservas WHERE id = ?", String.class, reservaId);
            assertEquals("CONFIRMADO", estado);
            assertEquals("APROBADO", jdbc.queryForObject(
                    "SELECT estado FROM sena_pagos WHERE reserva_id = ?", String.class, reservaId));

            // reintento de MP: mismo webhook otra vez → 200 y sin efectos
            assertEquals(200, exchange(HttpMethod.POST, "/public/pagos/mp/webhook?tenant=demo",
                    Map.of("type", "payment", "data", Map.of("id", "777")), publicHeaders(), Void.class)
                    .getStatusCode().value());
            assertEquals("CONFIRMADO", jdbc.queryForObject(
                    "SELECT estado FROM reservas WHERE id = ?", String.class, reservaId));
        } finally {
            setSena(false, null);
            credencialStore.eliminar(TENANT_DEMO);
        }
    }

    @Test
    void pagoAprobadoTardeQuedaRegistradoSinConfirmar() {
        conectarDemoStub();
        activarSena();
        try {
            long reservaId = crearReservaPendientePorApi(LocalDate.now().plusDays(36));
            exchange(HttpMethod.POST, "/public/pagos/mp/preferencia",
                    Map.of("reservaId", reservaId, "backUrl", "http://x/"), publicHeaders(), Map.class);

            // vencer la reserva a mano (patrón SenaIT de expiración perezosa)
            jdbc.update("UPDATE reservas SET expira_en = '2020-01-01 00:00:00' WHERE id = ?", reservaId);

            StubMpConfig.PAGO.set(new PagoMp("888", "approved", TENANT_DEMO + "-" + reservaId,
                    new BigDecimal("5000.00")));
            assertEquals(200, exchange(HttpMethod.POST, "/public/pagos/mp/webhook?tenant=demo",
                    Map.of("type", "payment", "data", Map.of("id", "888")), publicHeaders(), Void.class)
                    .getStatusCode().value());

            assertEquals("APROBADO_TARDE", jdbc.queryForObject(
                    "SELECT estado FROM sena_pagos WHERE reserva_id = ?", String.class, reservaId));
            assertNotEquals("CONFIRMADO", jdbc.queryForObject(
                    "SELECT estado FROM reservas WHERE id = ?", String.class, reservaId));
        } finally {
            setSena(false, null);
            credencialStore.eliminar(TENANT_DEMO);
        }
    }

    @Test
    void webhookNoTerminalNoQuemaIdempotencia() {
        conectarDemoStub();
        activarSena();
        try {
            long reservaId = crearReservaPendientePorApi(LocalDate.now().plusDays(37));
            exchange(HttpMethod.POST, "/public/pagos/mp/preferencia",
                    Map.of("reservaId", reservaId, "backUrl", "http://x/"), publicHeaders(), Map.class);

            // 1) in_process: no terminal → 200 sin registrar nada (payment_id queda libre)
            StubMpConfig.PAGO.set(new PagoMp("555", "in_process", TENANT_DEMO + "-" + reservaId,
                    new BigDecimal("5000.00")));
            assertEquals(200, exchange(HttpMethod.POST, "/public/pagos/mp/webhook?tenant=demo",
                    Map.of("type", "payment", "data", Map.of("id", "555")), publicHeaders(), Void.class)
                    .getStatusCode().value());
            assertEquals("PENDIENTE", jdbc.queryForObject(
                    "SELECT estado FROM sena_pagos WHERE reserva_id = ?", String.class, reservaId));
            assertNull(jdbc.queryForObject(
                    "SELECT payment_id FROM sena_pagos WHERE reserva_id = ?", String.class, reservaId));

            // 2) el MISMO pago pasa a approved → todavía procesable → confirma
            StubMpConfig.PAGO.set(new PagoMp("555", "approved", TENANT_DEMO + "-" + reservaId,
                    new BigDecimal("5000.00")));
            assertEquals(200, exchange(HttpMethod.POST, "/public/pagos/mp/webhook?tenant=demo",
                    Map.of("type", "payment", "data", Map.of("id", "555")), publicHeaders(), Void.class)
                    .getStatusCode().value());
            assertEquals("CONFIRMADO", jdbc.queryForObject(
                    "SELECT estado FROM reservas WHERE id = ?", String.class, reservaId));
            assertEquals("APROBADO", jdbc.queryForObject(
                    "SELECT estado FROM sena_pagos WHERE reserva_id = ?", String.class, reservaId));

            // 3) rechazo tardío de OTRO pago → no degrada la seña ya APROBADO
            StubMpConfig.PAGO.set(new PagoMp("556", "rejected", TENANT_DEMO + "-" + reservaId,
                    new BigDecimal("5000.00")));
            assertEquals(200, exchange(HttpMethod.POST, "/public/pagos/mp/webhook?tenant=demo",
                    Map.of("type", "payment", "data", Map.of("id", "556")), publicHeaders(), Void.class)
                    .getStatusCode().value());
            assertEquals("APROBADO", jdbc.queryForObject(
                    "SELECT estado FROM sena_pagos WHERE reserva_id = ?", String.class, reservaId));
            assertEquals("CONFIRMADO", jdbc.queryForObject(
                    "SELECT estado FROM reservas WHERE id = ?", String.class, reservaId));
        } finally {
            setSena(false, null);
            credencialStore.eliminar(TENANT_DEMO);
        }
    }

    // ---- Helpers de armado de una reserva PENDIENTE real (patrón de SenaIT) ----

    /** Conecta al tenant demo con una credencial de MP vigente (stub del gateway igual). */
    private void conectarDemoStub() {
        credencialStore.guardar(new CredencialMp(TENANT_DEMO, "111", "APP_USR-token-it", "TG-refresh-it",
                Instant.now().plus(180, ChronoUnit.DAYS)), "pub-key-it", "offline_access read write");
    }

    private void activarSena() {
        setSena(true, 5000);
    }

    private void setSena(boolean requiere, Integer monto) {
        Map<String, Object> body = new HashMap<>();
        body.put("requiereSena", requiere);
        body.put("senaMonto", monto);
        body.put("senaAlias", requiere ? "padel.hub.it" : null);
        ResponseEntity<String> resp = exchange(HttpMethod.PUT, "/api/v1/agenda/sena", body, ownerHeaders(), String.class);
        assertEquals(200, resp.getStatusCode().value());
    }

    /** Toma un slot libre real de /public/disponibilidad (no inventa horarios) y reserva ahí. */
    @SuppressWarnings("unchecked")
    private long crearReservaPendientePorApi(LocalDate fecha) {
        ResponseEntity<List> disp = exchange(HttpMethod.GET,
                "/public/disponibilidad?fecha=" + fecha + "&duracion=90", null, publicHeaders(), List.class);
        assertEquals(200, disp.getStatusCode().value());

        Map<String, Object> slot = ((List<Map<String, Object>>) disp.getBody()).stream()
                .filter(s -> Boolean.TRUE.equals(s.get("disponible")))
                .findFirst().orElseThrow(() -> new AssertionError("Sin slots libres el " + fecha));
        String hora = (String) slot.get("hora");
        List<Map<String, Object>> canchasLibres = (List<Map<String, Object>>) slot.get("canchasLibres");
        long canchaId = ((Number) canchasLibres.get(0).get("id")).longValue();

        Map<String, Object> body = new HashMap<>();
        body.put("complejoId", 1);
        body.put("canchaId", canchaId);
        body.put("fecha", fecha.toString());
        body.put("hora", hora);
        body.put("duracion", 90);
        body.put("clienteNombre", "Tester MP");
        body.put("clienteWhatsapp", "5493511" + fecha.toEpochDay());

        ResponseEntity<Map> creada = exchange(HttpMethod.POST, "/public/reservas", body, publicHeaders(), Map.class);
        assertEquals(201, creada.getStatusCode().value());
        return ((Number) creada.getBody().get("id")).longValue();
    }
}
