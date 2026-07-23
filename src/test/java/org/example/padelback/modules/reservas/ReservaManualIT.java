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
 * Reserva manual del dueño ({@code POST /api/v1/turnos}): nace CONFIRMADA aunque el complejo pida
 * seña, con teléfono opcional, sobre slots libres de la grilla; requiere rol OWNER.
 *
 * <p>Usa fecha +6 días y horas propias de la grilla de 90' para no chocar con los slots de las
 * otras clases IT (DB compartida por el singleton container).
 */
class ReservaManualIT extends IntegrationTestBase {

    private final String fecha = LocalDate.now().plusDays(6).toString();

    private Map<String, Object> manualBody(String hora, Long canchaId, String cliente, String tel) {
        Map<String, Object> body = new HashMap<>();
        body.put("complejoId", 1);
        body.put("canchaId", canchaId);
        body.put("fecha", fecha);
        body.put("hora", hora);
        body.put("duracion", 60);
        body.put("clienteNombre", cliente);
        body.put("clienteWhatsapp", tel);
        return body;
    }

    @SuppressWarnings("unchecked")
    @Test
    void manualNaceConfirmadaSinTelefonoYApareceEnElPanel() {
        String cliente = "Manual SinTel";
        ResponseEntity<Map> resp = exchange(HttpMethod.POST, "/api/v1/turnos",
                manualBody("08:00", 1L, cliente, null), ownerHeaders(), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().get("estado")).isEqualTo("CONFIRMADO");

        String turnos = exchange(HttpMethod.GET, "/api/v1/turnos?fecha=" + fecha,
                null, ownerHeaders(), String.class).getBody();
        assertThat(turnos).contains(cliente);
    }

    @Test
    void manualRequiereRolOwner() {
        ResponseEntity<String> resp = exchange(HttpMethod.POST, "/api/v1/turnos",
                manualBody("12:30", 1L, "Manual SinAuth", null), publicHeaders(), String.class);
        assertThat(resp.getStatusCode().value()).isIn(401, 403);
    }

    @SuppressWarnings("unchecked")
    @Test
    void manualSobreSlotOcupadoDa409() {
        // Ocupa el slot por el camino público (con teléfono, que el público sí exige).
        Map<String, Object> publica = manualBody("14:00", 1L, "Ocupante Publico", "5493510008888");
        ResponseEntity<Map> creada = exchange(HttpMethod.POST, "/public/reservas", publica, publicHeaders(), Map.class);
        assertThat(creada.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> manual = exchange(HttpMethod.POST, "/api/v1/turnos",
                manualBody("14:00", 1L, "Manual Choca", null), ownerHeaders(), String.class);
        assertThat(manual.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @SuppressWarnings("unchecked")
    @Test
    void manualConSenaActivaIgualNaceConfirmada() {
        Map<String, Object> sena = new HashMap<>();
        sena.put("requiereSena", true);
        sena.put("senaMonto", 5000);
        sena.put("senaAlias", "padel.hub.manual");
        assertThat(exchange(HttpMethod.PUT, "/api/v1/agenda/sena", sena, ownerHeaders(), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        try {
            ResponseEntity<Map> resp = exchange(HttpMethod.POST, "/api/v1/turnos",
                    manualBody("15:30", 2L, "Manual ConSena", null), ownerHeaders(), Map.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            // La pública nacería PENDIENTE; la manual del dueño se confirma directo.
            assertThat(resp.getBody().get("estado")).isEqualTo("CONFIRMADO");
        } finally {
            Map<String, Object> off = new HashMap<>();
            off.put("requiereSena", false);
            off.put("senaMonto", null);
            off.put("senaAlias", null);
            exchange(HttpMethod.PUT, "/api/v1/agenda/sena", off, ownerHeaders(), String.class);
        }
    }

    @Test
    void disponibilidadOwnerDevuelveSlots() {
        ResponseEntity<String> resp = exchange(HttpMethod.GET,
                "/api/v1/turnos/disponibilidad?fecha=" + fecha + "&duracion=60",
                null, ownerHeaders(), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"hora\"").contains("canchasLibres");

        // Sin JWT es un endpoint del panel: no responde.
        ResponseEntity<String> anon = exchange(HttpMethod.GET,
                "/api/v1/turnos/disponibilidad?fecha=" + fecha + "&duracion=60",
                null, publicHeaders(), String.class);
        assertThat(anon.getStatusCode().value()).isIn(401, 403);
    }

    @SuppressWarnings("unchecked")
    @Test
    void manualSinCanchaAutoasigna() {
        ResponseEntity<Map> resp = exchange(HttpMethod.POST, "/api/v1/turnos",
                manualBody("17:00", null, "Manual Cualquiera", null), ownerHeaders(), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(((Number) resp.getBody().get("canchaId")).longValue()).isPositive();
    }
}
