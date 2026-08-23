package org.example.padelback.modules.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.example.padelback.infrastructure.tenancy.PublicTenantContextFilter;
import org.example.padelback.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * El login en dos pasos, por HTTP real. Lo que se está protegiendo acá es que el apex no necesite
 * saber de qué club es quien entra, y que el código que emite no valga en el host equivocado.
 */
class LoginApexIT extends IntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    /**
     * Un segundo club, para probar que un código no se canjea en el host de otro.
     *
     * Slug propio (`apexotro`) y NO el `otro` de `TenancyIT`, aunque la tentación sea reusarlo:
     * todos los IT comparten la misma base y ese seed es idempotente por slug. Si esta clase
     * corriera primero y sembrara un `otro` sin complejo ni canchas, `TenancyIT` encontraría el
     * slug ocupado, se saltearía su propio seed y fallaría buscando "Cancha Otra" — un rojo en el
     * archivo equivocado, que depende del orden en que JUnit corra las clases.
     *
     * `id` fijo y ALTO (900), no auto-increment: `TenancyIT` hardcodea `id = 2` para su propio
     * tenant y lo encadena a sus `complejos` y `canchas`. Si acá se dejara elegir al
     * auto-increment, esta clase —que corre antes por orden alfabético— se llevaría el 2 y
     * `TenancyIT` explotaría con `Duplicate entry '2'`. Con el 900 los dos órdenes funcionan: el 2
     * queda libre, y el auto-increment posterior arranca del 901.
     *
     * Sin fila en `tenant_dominios`: el canje resuelve el tenant por el header `X-Tenant`, que va
     * por slug.
     */
    @BeforeEach
    void seedClubAjeno() {
        Integer existe = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tenants WHERE slug = 'apexotro'", Integer.class);
        if (existe != null && existe > 0) return;

        jdbc.update("INSERT INTO tenants (id, slug, name, status, color_primario, fuente, "
                + "mostrar_precios, requiere_telefono, created_at, updated_at) "
                + "VALUES (900, 'apexotro', 'Club Ajeno', 'ACTIVE', '#000000', 'Hanken Grotesk', TRUE, TRUE, NOW(6), NOW(6))");
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> postLogin(String email, String password) {
        return exchange(HttpMethod.POST, "/api/v1/auth/login",
                Map.of("email", email, "password", password), jsonHeaders(), Map.class);
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> postCanje(String code, String slug) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(PublicTenantContextFilter.TENANT_HEADER, slug);
        return exchange(HttpMethod.POST, "/api/v1/auth/canjear", Map.of("code", code), headers, Map.class);
    }

    @Test
    void loginSinHeaderDeTenant_devuelveElClubDelEmailYUnCodigo() {
        var resp = postLogin(OWNER_EMAIL, OWNER_PASSWORD);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("slug")).isEqualTo(TENANT);
        assertThat((String) resp.getBody().get("code")).isNotBlank();
        // El JWT NO sale por acá: el apex no puede escribir en el localStorage del club.
        assertThat(resp.getBody()).doesNotContainKey("token");
    }

    @Test
    void elCodigoSeCanjeaPorUnJwtQueSirveDeVerdad() {
        String code = (String) postLogin(OWNER_EMAIL, OWNER_PASSWORD).getBody().get("code");

        var canje = postCanje(code, TENANT);
        assertThat(canje.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = (String) canje.getBody().get("token");
        assertThat(token).isNotBlank();

        HttpHeaders conBearer = publicHeaders();
        conBearer.setBearerAuth(token);
        var me = exchange(HttpMethod.GET, "/api/v1/auth/me", null, conBearer, Map.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody().get("email")).isEqualTo(OWNER_EMAIL);
    }

    @Test
    void elMismoCodigoNoSeCanjeaDosVeces() {
        String code = (String) postLogin(OWNER_EMAIL, OWNER_PASSWORD).getBody().get("code");
        postCanje(code, TENANT);

        assertThat(postCanje(code, TENANT).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void unCodigoNoSeCanjeaEnElHostDeOtroClub() {
        String code = (String) postLogin(OWNER_EMAIL, OWNER_PASSWORD).getBody().get("code");

        assertThat(postCanje(code, "apexotro").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        // Y además quedó quemado: no se puede ir probando host por host hasta acertar.
        assertThat(postCanje(code, TENANT).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void emailInexistenteYPasswordIncorrecta_respondenExactamenteLoMismo() {
        // La invariante de §5.4 de la spec: el apex es un buscador global de emails y la diferencia
        // entre "no existe" y "contraseña mala" diría qué mails son clientes de Padel-HUB.
        var passwordMala = postLogin(OWNER_EMAIL, "no-es-la-password");
        var emailInexistente = postLogin("nadie@ningun-club.com", "no-es-la-password");

        assertThat(passwordMala.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(emailInexistente.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(emailInexistente.getBody()).isEqualTo(passwordMala.getBody());
    }

    @Test
    void elEmailNoDistingueMayusculas() {
        var resp = postLogin(OWNER_EMAIL.toUpperCase(), OWNER_PASSWORD);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("slug")).isEqualTo(TENANT);
    }
}
