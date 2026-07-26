package org.example.padelback.modules.reservas;

import org.example.padelback.modules.reservas.infrastructure.scheduling.PurgaReservasViejasJob;
import org.example.padelback.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PurgaRetencionIT extends IntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PurgaReservasViejasJob job;

    @Test
    void purgaSenaPagosHuerfanosYArrepentimientosGestionadosViejos() {
        // sena_pago huérfano: apunta a una reserva que no existe
        jdbc.update("INSERT INTO sena_pagos (reserva_id, tenant_id, preference_id, init_point, estado, monto, updated_at) "
                + "VALUES (999999901, 1, 'pref-huerfana', 'https://x', 'APROBADO', 5000.00, NOW(6))");
        // arrepentimiento gestionado viejo (2 años) y uno pendiente viejo (debe sobrevivir)
        jdbc.update("INSERT INTO arrepentimientos (tenant_id, codigo, nombre, whatsapp, gestionado, "
                + "created_at, updated_at, created_by, updated_by, active, version) "
                + "VALUES (1, 'ARR-PURGA1', 'Viejo Gestionado', '111', 1, "
                + "DATE_SUB(NOW(6), INTERVAL 730 DAY), NOW(6), 'test', 'test', 1, 0)");
        jdbc.update("INSERT INTO arrepentimientos (tenant_id, codigo, nombre, whatsapp, gestionado, "
                + "created_at, updated_at, created_by, updated_by, active, version) "
                + "VALUES (1, 'ARR-PURGA2', 'Viejo Pendiente', '222', 0, "
                + "DATE_SUB(NOW(6), INTERVAL 730 DAY), NOW(6), 'test', 'test', 1, 0)");

        job.purgar();

        assertEquals(0, contar("SELECT COUNT(*) FROM sena_pagos WHERE reserva_id = 999999901"));
        assertEquals(0, contar("SELECT COUNT(*) FROM arrepentimientos WHERE codigo = 'ARR-PURGA1'"));
        assertEquals(1, contar("SELECT COUNT(*) FROM arrepentimientos WHERE codigo = 'ARR-PURGA2'")); // pendiente sobrevive
        jdbc.update("DELETE FROM arrepentimientos WHERE codigo IN ('ARR-PURGA1','ARR-PURGA2')"); // limpieza
    }

    private int contar(String sql) {
        Integer n = jdbc.queryForObject(sql, Integer.class);
        return n == null ? 0 : n;
    }
}
