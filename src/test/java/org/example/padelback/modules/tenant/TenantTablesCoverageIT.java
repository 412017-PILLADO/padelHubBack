package org.example.padelback.modules.tenant;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.example.padelback.modules.tenant.application.TenantAdminService;
import org.example.padelback.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guardián: toda tabla con columna tenant_id DEBE estar en la lista CASCADE de la baja de tenant.
 * Este test falla solo en cuanto alguien agrega una tabla tenant-scoped sin actualizar la baja
 * (el bug que V15 introdujo y el review final atrapó a mano).
 */
class TenantTablesCoverageIT extends IntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void todaTablaConTenantIdEstaEnElCascadeDeBaja() {
        List<String> conTenantId = jdbc.queryForList(
                "SELECT LOWER(table_name) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND column_name = 'tenant_id'",
                String.class);

        Set<String> faltantes = new HashSet<>(conTenantId);
        TenantAdminService.tablasCascade().forEach(t -> faltantes.remove(t.toLowerCase()));
        assertTrue(faltantes.isEmpty(),
                "Tablas con tenant_id fuera del CASCADE de baja de tenant (agregarlas en "
                        + "TenantAdminService.CASCADE): " + faltantes);
    }

    @Test
    void elCascadeNoNombraTablasInexistentes() {
        List<String> existentes = jdbc.queryForList(
                "SELECT LOWER(table_name) FROM information_schema.tables WHERE table_schema = DATABASE()",
                String.class);
        Set<String> fantasma = new HashSet<>();
        for (String t : TenantAdminService.tablasCascade()) {
            if (!existentes.contains(t.toLowerCase())) {
                fantasma.add(t);
            }
        }
        assertTrue(fantasma.isEmpty(), "CASCADE nombra tablas que no existen (typo/rename): " + fantasma);
    }
}
