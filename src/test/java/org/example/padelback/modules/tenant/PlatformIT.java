package org.example.padelback.modules.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.example.padelback.infrastructure.tenancy.PublicTenantContextFilter;
import org.example.padelback.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Plataforma (super-admin): login, guard de {@code /platform/**}, alta/listado/edición/baja de
 * clubes y el campo plantilla — incluyendo que el alta deja la config inicial completa y que la baja
 * borra todo. También valida que el OWNER pueda cambiar su plantilla por el endpoint de marca.
 */
class PlatformIT extends IntegrationTestBase {

    private static final String ADMIN_EMAIL = "admin@padelhub.com";
    private static final String ADMIN_PASSWORD = "superadmin123";

    private String platformToken;

    // ── helpers ──────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private String platformToken() {
        if (platformToken == null) {
            ResponseEntity<Map> resp = exchange(HttpMethod.POST, "/platform/auth/login",
                    Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD), jsonHeaders(), Map.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            platformToken = (String) resp.getBody().get("token");
        }
        return platformToken;
    }

    /** Headers JSON pelados (sin tenant): la plataforma no es tenant-scoped. */
    private HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    /** Headers de plataforma: Bearer del super-admin, sin X-Tenant. */
    private HttpHeaders platformHeaders() {
        HttpHeaders h = jsonHeaders();
        h.setBearerAuth(platformToken());
        return h;
    }

    /** Headers públicos apuntando a un tenant arbitrario por slug. */
    private HttpHeaders publicHeadersFor(String slug) {
        HttpHeaders h = jsonHeaders();
        h.set(PublicTenantContextFilter.TENANT_HEADER, slug);
        return h;
    }

    @SuppressWarnings("unchecked")
    private long crearClub(String slug, String plantilla) {
        Map<String, Object> body = new HashMap<>();
        body.put("slug", slug);
        body.put("name", "Club " + slug);
        body.put("colorPrimario", "#17b26a");
        body.put("colorSecundario", "#f5a623");
        body.put("plantilla", plantilla);
        body.put("ownerEmail", "owner@" + slug + ".com");
        body.put("ownerPassword", "padel123");
        body.put("direccion", "Calle 1");
        body.put("whatsapp", "5493410000000");
        body.put("hosts", List.of(slug + ".localhost"));
        ResponseEntity<Map> resp = exchange(HttpMethod.POST, "/platform/tenants", body, platformHeaders(), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) resp.getBody().get("tenantId")).longValue();
    }

    private String configDe(String slug) {
        return exchange(HttpMethod.GET, "/public/config", null, publicHeadersFor(slug), String.class).getBody();
    }

    // ── auth / guard ─────────────────────────────────────────────────
    @Test
    void loginSuperAdminDevuelveToken() {
        assertThat(platformToken()).isNotBlank();
    }

    @Test
    void loginInvalidoRechaza() {
        ResponseEntity<String> resp = exchange(HttpMethod.POST, "/platform/auth/login",
                Map.of("email", ADMIN_EMAIL, "password", "mala"), jsonHeaders(), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void listadoSinTokenRechaza() {
        ResponseEntity<String> resp = exchange(HttpMethod.GET, "/platform/tenants", null, jsonHeaders(), String.class);
        assertThat(resp.getStatusCode().value()).isIn(401, 403);
    }

    // ── alta / plantilla ─────────────────────────────────────────────
    @Test
    void altaDejaConfigInicialCompletaYPlantilla() {
        crearClub("italta", "B");

        // El listado de plataforma lo muestra con su plantilla.
        String listado = exchange(HttpMethod.GET, "/platform/tenants", null, platformHeaders(), String.class).getBody();
        assertThat(listado).contains("italta").contains("\"plantilla\":\"B\"");

        // Config pública: plantilla B + complejo con 2 canchas + horarios + duraciones (alta completa).
        String cfg = configDe("italta");
        assertThat(cfg).contains("\"plantilla\":\"B\"");
        assertThat(cfg).contains("Cancha 1").contains("Cancha 2");
        assertThat(cfg).contains("\"duracionesPermitidas\"");
        assertThat(cfg).contains("\"horarios\"");
    }

    @Test
    void plantillaInvalidaEnAltaCaeADefaultA() {
        crearClub("itdefault", "Z"); // 'Z' no es A/B/C → el provisioning la normaliza a 'A'
        assertThat(configDe("itdefault")).contains("\"plantilla\":\"A\"");
    }

    @Test
    void editarCambiaLaPlantilla() {
        long id = crearClub("itedit", "A");

        ResponseEntity<String> resp = exchange(HttpMethod.PUT, "/platform/tenants/" + id,
                Map.of("plantilla", "C"), platformHeaders(), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"plantilla\":\"C\"");
        assertThat(configDe("itedit")).contains("\"plantilla\":\"C\"");
    }

    // ── baja ─────────────────────────────────────────────────────────
    @Test
    void bajaEliminaElClubYSusDatos() {
        long id = crearClub("itbaja", "A");
        assertThat(configDe("itbaja")).contains("Cancha 1");

        ResponseEntity<Void> del = exchange(HttpMethod.DELETE, "/platform/tenants/" + id, null,
                platformHeaders(), Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        String listado = exchange(HttpMethod.GET, "/platform/tenants", null, platformHeaders(), String.class).getBody();
        assertThat(listado).doesNotContain("itbaja");
    }

    @Test
    void bajaDeClubInexistenteDa404() {
        ResponseEntity<String> resp = exchange(HttpMethod.DELETE, "/platform/tenants/999999", null,
                platformHeaders(), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── seguridad ────────────────────────────────────────────────────
    @Test
    void altaConHostYaUsadoDa409() {
        crearClub("ithosta", "A"); // reclama el host ithosta.localhost

        Map<String, Object> body = new HashMap<>();
        body.put("slug", "ithostb");
        body.put("name", "Host B");
        body.put("ownerEmail", "owner@ithostb.com");
        body.put("ownerPassword", "padel123");
        body.put("hosts", List.of("ithosta.localhost")); // host ya tomado por otro club
        ResponseEntity<String> resp = exchange(HttpMethod.POST, "/platform/tenants", body,
                platformHeaders(), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        // Y no dejó a medias el club nuevo (rollback de la transacción).
        String listado = exchange(HttpMethod.GET, "/platform/tenants", null, platformHeaders(), String.class).getBody();
        assertThat(listado).doesNotContain("ithostb");
    }

    @Test
    void loginBruteForceBloqueaCon429() {
        // Email inexistente y dedicado: no toca al admin real. Tope de PlatformLoginThrottle = 5.
        String email = "bruteforce@none.test";
        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> fail = exchange(HttpMethod.POST, "/platform/auth/login",
                    Map.of("email", email, "password", "mala"), jsonHeaders(), String.class);
            assertThat(fail.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
        ResponseEntity<String> locked = exchange(HttpMethod.POST, "/platform/auth/login",
                Map.of("email", email, "password", "mala"), jsonHeaders(), String.class);
        assertThat(locked.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    // ── owner cambia su plantilla por marca ──────────────────────────
    @Test
    void ownerCambiaPlantillaPorMarca() {
        try {
            ResponseEntity<String> resp = exchange(HttpMethod.PUT, "/api/v1/agenda/marca",
                    Map.of("colorPrimario", "#2747ff", "plantilla", "B"), ownerHeaders(), String.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody()).contains("\"plantilla\":\"B\"");
            assertThat(configDe(TENANT)).contains("\"plantilla\":\"B\"");
        } finally {
            // Dejar demo como estaba (plantilla A) para no afectar otras clases IT.
            exchange(HttpMethod.PUT, "/api/v1/agenda/marca",
                    Map.of("colorPrimario", "#2747ff", "plantilla", "A"), ownerHeaders(), String.class);
        }
    }

    @Test
    void plantillaInvalidaEnMarcaDa400() {
        ResponseEntity<String> resp = exchange(HttpMethod.PUT, "/api/v1/agenda/marca",
                Map.of("colorPrimario", "#2747ff", "plantilla", "Z"), ownerHeaders(), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
