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
 * Flujo público de reserva: happy path, anti-doble-reserva (lock) y honeypot.
 *
 * <p>Cada test usa una fecha distinta: las clases IT comparten una sola DB (singleton container) y
 * las reservas de 90' se solapan entre horas contiguas, así que aislar por fecha evita colisiones.
 */
class ReservaIT extends IntegrationTestBase {

    private Map<String, Object> reservaBody(String fecha, String hora, Long canchaId, String tel, String honeypot) {
        Map<String, Object> body = new HashMap<>();
        body.put("complejoId", 1);
        body.put("canchaId", canchaId);
        body.put("fecha", fecha);
        body.put("hora", hora);
        body.put("duracion", 90);
        body.put("clienteNombre", "Tester");
        body.put("clienteWhatsapp", tel);
        if (honeypot != null) body.put("empresa", honeypot);
        return body;
    }

    @SuppressWarnings("unchecked")
    @Test
    void happyPathCreaReserva() {
        String fecha = LocalDate.now().plusDays(3).toString();
        // 09:30 es un inicio válido de la grilla anclada al turno principal de 90 (08:00, 09:30, 11:00…).
        ResponseEntity<Map> resp = exchange(HttpMethod.POST, "/public/reservas",
                reservaBody(fecha, "09:30", 1L, "5493510000001", null), publicHeaders(), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).containsKey("canchaNombre");
        assertThat(resp.getBody().get("estado")).isEqualTo("CONFIRMADO");
    }

    @Test
    void dobleReservaDeLaMismaCanchaYSlotDa409() {
        String fecha = LocalDate.now().plusDays(5).toString();
        ResponseEntity<String> primera = exchange(HttpMethod.POST, "/public/reservas",
                reservaBody(fecha, "11:00", 1L, "5493510000002", null), publicHeaders(), String.class);
        assertThat(primera.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // mismo slot y cancha, otro teléfono (para no toparse con el límite por teléfono)
        ResponseEntity<String> segunda = exchange(HttpMethod.POST, "/public/reservas",
                reservaBody(fecha, "11:00", 1L, "5493510000003", null), publicHeaders(), String.class);
        assertThat(segunda.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void honeypotRechazaLaSolicitud() {
        String fecha = LocalDate.now().plusDays(7).toString();
        ResponseEntity<String> resp = exchange(HttpMethod.POST, "/public/reservas",
                reservaBody(fecha, "12:00", 1L, "5493510000004", "soy-un-bot"), publicHeaders(), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
