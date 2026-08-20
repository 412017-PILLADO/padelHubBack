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
        // 09:30 / 11:00 son inicios válidos de la grilla anclada al turno principal de 90.
        crearReserva("09:30", cliente);

        String body = turnos();
        assertThat(body).contains(cliente);
        assertThat(body).contains("\"hora\"").contains("\"fin\"");
        assertThat(body).doesNotContain("horaInicio").doesNotContain("horaFin");
    }

    /**
     * El panel arma la grilla ubicando cada turno en la columna de SU cancha. Sin {@code canchaId}
     * tenía que hacerlo por nombre, que no identifica nada: dos canchas homónimas caen en la misma
     * columna y una cancha dada de baja deja de tener nombre resoluble.
     */
    @Test
    void listadoTraeElIdDeLaCancha() {
        crearReserva("12:30", "Cliente ConIdDeCancha");

        assertThat(turnos()).contains("\"canchaId\":1");
    }

    /**
     * Baja de cancha con reservas ya hechas: es soft-delete justamente para no perderlas, así que el
     * panel las tiene que seguir mostrando CON el nombre de su cancha. Antes el listado resolvía
     * nombres mirando sólo canchas activas y esos turnos salían con la cancha en "—".
     */
    @Test
    @SuppressWarnings("unchecked")
    void turnoDeUnaCanchaDadaDeBajaConservaSuNombre() {
        Map<String, Object> alta = Map.of(
                "nombre", "Cancha Con Historia", "techada", true, "tipoPared", "CRISTAL", "precioHora", 5000);
        ResponseEntity<Map> creada = exchange(
                HttpMethod.POST, "/api/v1/agenda/canchas", alta, ownerHeaders(), Map.class);
        assertThat(creada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long canchaId = ((Number) creada.getBody().get("id")).longValue();

        Map<String, Object> reserva = new HashMap<>();
        reserva.put("canchaId", canchaId);
        reserva.put("fecha", fecha);
        reserva.put("hora", "14:00");
        reserva.put("duracion", 60);
        reserva.put("clienteNombre", "Cliente De Cancha Baja");
        ResponseEntity<Map> resp = exchange(
                HttpMethod.POST, "/api/v1/turnos", reserva, ownerHeaders(), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        exchange(HttpMethod.DELETE, "/api/v1/agenda/canchas/" + canchaId, null, ownerHeaders(), Void.class);

        String body = turnos();
        assertThat(body).contains("Cliente De Cancha Baja");
        assertThat(body).contains("Cancha Con Historia");
    }

    @Test
    void cancelarSacaElTurnoDelListado() {
        String cliente = "Cliente Cancelable";
        Long id = crearReserva("11:00", cliente);
        assertThat(turnos()).contains(cliente);

        ResponseEntity<String> cancel = exchange(
                HttpMethod.POST, "/api/v1/turnos/" + id + "/cancelar", null, ownerHeaders(), String.class);
        assertThat(cancel.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancel.getBody()).contains("CANCELADO");

        assertThat(turnos()).doesNotContain(cliente);
    }
}
