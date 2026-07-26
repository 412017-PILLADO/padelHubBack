package org.example.padelback.modules.pagos;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.example.padelback.modules.pagos.domain.model.CredencialMp;
import org.example.padelback.modules.pagos.infrastructure.persistence.TenantMercadoPagoStore;
import org.example.padelback.modules.pagos.infrastructure.persistence.SenaPagoStore;
import org.example.padelback.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
}
