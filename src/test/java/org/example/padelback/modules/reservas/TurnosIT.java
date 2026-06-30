package org.example.padelback.modules.reservas;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.example.padelback.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Panel de turnos: el contrato del listado usa {@code hora}/{@code fin} (regresión del bug ya
 * arreglado, donde el back devolvía horaInicio/horaFin y el front esperaba hora/fin) y la cancelación.
 *
 * <p>Cada test usa una hora y un nombre de cliente distintos: las clases IT comparten una sola DB
 * (singleton container), así que reservar el mismo slot en dos tests chocaría (409).
 */
class TurnosIT extends IntegrationTestBase {

    private final String fecha = LocalDate.now().plusDays(4).toString();

    @SuppressWarnings("unchecked")
    private Long crearReserva(String hora, String cliente) {
        Map<String, Object> body = new HashMap<>();
        body.put("complejoId", 1);
        body.put("canchaId", 1);
        body.put("fecha", fecha);
        body.put("hora", hora);
        body.put("duracion", 60);
        body.put("clienteNombre", cliente);
        body.put("clienteWhatsapp", "5493510009999");
        ResponseEntity<Map> resp = exchange(
                HttpMethod.POST, "/public/reservas", body, publicHeaders(), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) resp.getBody().get("id")).longValue();
    }

    private String turnos() {
        return exchange(HttpMethod.GET, "/api/v1/turnos?fecha=" + fecha,
                null, ownerHeaders(), String.class).getBody();
    }

    @Test
    void listadoUsaCamposHoraYFin() {
        String cliente = "Cliente HoraFin";
        crearReserva("09:00", cliente);

        String body = turnos();
        assertThat(body).contains(cliente);
        assertThat(body).contains("\"hora\"").contains("\"fin\"");
        assertThat(body).doesNotContain("horaInicio").doesNotContain("horaFin");
    }

    @Test
    void cancelarSacaElTurnoDelListado() {
        String cliente = "Cliente Cancelable";
        Long id = crearReserva("10:00", cliente);
        assertThat(turnos()).contains(cliente);

        ResponseEntity<String> cancel = exchange(
                HttpMethod.POST, "/api/v1/turnos/" + id + "/cancelar", null, ownerHeaders(), String.class);
        assertThat(cancel.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancel.getBody()).contains("CANCELADO");

        assertThat(turnos()).doesNotContain(cliente);
    }
}
