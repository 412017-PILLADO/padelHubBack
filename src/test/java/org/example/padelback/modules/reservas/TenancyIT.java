package org.example.padelback.modules.reservas;

import static org.assertj.core.api.Assertions.assertThat;

import org.example.padelback.infrastructure.tenancy.PublicTenantContextFilter;
import org.example.padelback.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Aislamiento entre tenants: la config pública de un tenant nunca expone las canchas de otro.
 * Se siembra un segundo tenant 'otro' por SQL directo (bypasea el filtro de Hibernate a propósito,
 * como haría el alta de un tenant) y se comprueba que cada slug ve sólo lo suyo a través del path
 * real de queries (que aplica el {@code tenantFilter}).
 */
class TenancyIT extends IntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void seedSegundoTenant() {
        Integer existe = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tenants WHERE slug = 'otro'", Integer.class);
        if (existe != null && existe > 0) return;

        jdbc.update("INSERT INTO tenants (id, slug, name, status, color_primario, fuente, "
                + "mostrar_precios, requiere_telefono, created_at, updated_at) "
                + "VALUES (2, 'otro', 'Otro Club', 'ACTIVE', '#000000', 'Hanken Grotesk', TRUE, TRUE, NOW(6), NOW(6))");
        jdbc.update("INSERT INTO tenant_dominios (tenant_id, host) VALUES (2, 'otro.localhost')");
        jdbc.update("INSERT INTO complejos (id, tenant_id, nombre, paso_minutos, duraciones_permitidas, "
                + "duracion_default, estado, active, created_at, updated_at, created_by, updated_by, version) "
                + "VALUES (2, 2, 'Complejo Otro', 30, '60,90,120', 90, 'ACTIVO', TRUE, NOW(6), NOW(6), 'seed', 'seed', 0)");
        jdbc.update("INSERT INTO canchas (tenant_id, complejo_id, nombre, orden, techada, tipo_pared, "
                + "estado, active, created_at, updated_at, created_by, updated_by, version) "
                + "VALUES (2, 2, 'Cancha Otra', 1, TRUE, 'CRISTAL', 'ACTIVO', TRUE, NOW(6), NOW(6), 'seed', 'seed', 0)");
    }

    private String configDe(String slug) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(PublicTenantContextFilter.TENANT_HEADER, slug);
        return exchange(HttpMethod.GET, "/public/config", null, headers, String.class).getBody();
    }

    @Test
    void cadaTenantVeSoloSusCanchas() {
        String demo = configDe("demo");
        assertThat(demo).contains("Cancha 1");
        assertThat(demo).doesNotContain("Cancha Otra");

        String otro = configDe("otro");
        assertThat(otro).contains("Cancha Otra");
        assertThat(otro).doesNotContain("Cancha 1");
    }
}
