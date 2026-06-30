package org.example.padelback.support;

import java.util.Map;

import org.example.padelback.infrastructure.tenancy.PublicTenantContextFilter;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.MySQLContainer;

/**
 * Base de los tests de integración: levanta el backend real (RANDOM_PORT) contra un MySQL en
 * Testcontainers, con Flyway aplicando schema + seed demo. Expone helpers para autenticarse como
 * el owner del tenant {@code demo} y para armar headers públicos/autenticados.
 *
 * <p><b>Singleton container:</b> el MySQL se arranca una sola vez (bloque static) y NO se frena por
 * clase. Spring cachea y reusa el contexto entre clases IT; si cada clase tuviera su propio
 * {@code @Container} (que se frena en su @AfterAll), la siguiente clase reusaría el contexto con un
 * HikariPool apuntando a un contenedor muerto. Ryuk limpia el contenedor al terminar la JVM.
 *
 * <p>El perfil {@code test} (application-test.yml) sube los límites anti-abuso: todas las clases
 * comparten la misma DB y las reservas se acumularían por IP/teléfono entre tests.
 *
 * <p>El owner (owner@padelhub.com / padel123) lo siembra OwnerSeeder al arrancar (perfil != prod).
 * El cliente HTTP nunca lanza por status 4xx/5xx: los tests inspeccionan el código directamente.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    protected static final String TENANT = "demo";
    protected static final String OWNER_EMAIL = "owner@padelhub.com";
    protected static final String OWNER_PASSWORD = "padel123";

    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    static {
        MYSQL.start();
    }

    @LocalServerPort
    protected int port;

    protected RestClient client;
    protected String ownerToken;

    @BeforeEach
    void setUp() {
        if (client == null) {
            client = RestClient.builder()
                    .baseUrl("http://localhost:" + port)
                    // No lanzar por error: que el test lea el status (401/403/409/400…) él mismo.
                    .defaultStatusHandler(status -> true, (request, response) -> { })
                    .build();
        }
        if (ownerToken == null) {
            ownerToken = login(OWNER_EMAIL, OWNER_PASSWORD);
        }
    }

    /** Login real por HTTP; resuelve el tenant por el header X-Tenant. Devuelve el JWT. */
    @SuppressWarnings("unchecked")
    protected String login(String email, String password) {
        ResponseEntity<Map> resp = exchange(HttpMethod.POST, "/api/v1/auth/login",
                Map.of("email", email, "password", password), publicHeaders(), Map.class);
        return (String) resp.getBody().get("token");
    }

    /** Headers de la superficie pública: sólo el slug del tenant. */
    protected HttpHeaders publicHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(PublicTenantContextFilter.TENANT_HEADER, TENANT);
        return headers;
    }

    /** Headers de panel: Bearer del owner + slug del tenant. */
    protected HttpHeaders ownerHeaders() {
        HttpHeaders headers = publicHeaders();
        headers.setBearerAuth(ownerToken);
        return headers;
    }

    protected <T> ResponseEntity<T> exchange(HttpMethod method, String url, Object body,
                                             HttpHeaders headers, Class<T> type) {
        RestClient.RequestBodySpec spec = client.method(method).uri(url)
                .headers(h -> h.addAll(headers));
        if (body != null) {
            spec = spec.body(body);
        }
        return spec.retrieve().toEntity(type);
    }
}
