package org.example.padelback.modules.reservas;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;

import org.example.padelback.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Alta/edición/baja de canchas desde el panel, y su efecto en config y disponibilidad pública. */
class CanchaCrudIT extends IntegrationTestBase {

    private String disponibilidad() {
        String fecha = LocalDate.now().plusDays(2).toString();
        return exchange(HttpMethod.GET,
                "/public/disponibilidad?fecha=" + fecha + "&duracion=90",
                null, publicHeaders(), String.class).getBody();
    }

    private String config() {
        return exchange(HttpMethod.GET, "/api/v1/agenda/config", null, ownerHeaders(), String.class).getBody();
    }

    @SuppressWarnings("unchecked")
    private Long crearCancha(String nombre) {
        Map<String, Object> body = Map.of(
                "nombre", nombre, "techada", true, "tipoPared", "CRISTAL", "precioHora", 5500);
        ResponseEntity<Map> resp = exchange(
                HttpMethod.POST, "/api/v1/agenda/canchas", body, ownerHeaders(), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) resp.getBody().get("id")).longValue();
    }

    @Test
    void altaApareceEnConfigYEnDisponibilidad() {
        Long id = crearCancha("Cancha Nueva");

        assertThat(config()).contains("Cancha Nueva");
        assertThat(disponibilidad()).contains("Cancha Nueva");
        assertThat(id).isPositive();
    }

    @Test
    void edicionCambiaElNombre() {
        Long id = crearCancha("Cancha Vieja");

        Map<String, Object> body = Map.of(
                "nombre", "Cancha Renombrada", "techada", false, "tipoPared", "MURO",
                "precioHora", 6000, "estado", "ACTIVO");
        ResponseEntity<String> resp = exchange(
                HttpMethod.PUT, "/api/v1/agenda/canchas/" + id, body, ownerHeaders(), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        String config = config();
        assertThat(config).contains("Cancha Renombrada");
        assertThat(config).doesNotContain("Cancha Vieja");
    }

    @Test
    void bajaDesapareceDeDisponibilidadNueva() {
        Long id = crearCancha("Cancha Efimera");
        assertThat(disponibilidad()).contains("Cancha Efimera");

        ResponseEntity<Void> del = exchange(
                HttpMethod.DELETE, "/api/v1/agenda/canchas/" + id, null, ownerHeaders(), Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(disponibilidad()).doesNotContain("Cancha Efimera");
        assertThat(config()).doesNotContain("Cancha Efimera");
    }

    @Test
    void sinTokenRechaza() {
        Map<String, Object> body = Map.of("nombre", "X", "techada", true, "tipoPared", "CRISTAL");
        HttpHeaders sinAuth = publicHeaders(); // sin Bearer
        ResponseEntity<String> resp = exchange(
                HttpMethod.POST, "/api/v1/agenda/canchas", body, sinAuth, String.class);

        assertThat(resp.getStatusCode().value()).isIn(401, 403);
    }

    @Test
    void tipoParedInvalidoDa400() {
        Map<String, Object> body = Map.of("nombre", "Mala", "techada", true, "tipoPared", "LADRILLO");
        ResponseEntity<String> resp = exchange(
                HttpMethod.POST, "/api/v1/agenda/canchas", body, ownerHeaders(), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
