package org.example.padelback.modules.reservas;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.example.padelback.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Config de precios (general vs por cancha) y del turno principal (permitir o no otras duraciones),
 * y su efecto en la superficie pública. Cada test deja el complejo en su estado demo original
 * (precio por cancha, otras duraciones permitidas) porque las clases IT comparten una sola DB.
 */
class PreciosYDuracionesIT extends IntegrationTestBase {

    @SuppressWarnings("unchecked")
    private Map<String, Object> publicConfig() {
        return exchange(HttpMethod.GET, "/public/config", null, publicHeaders(), Map.class).getBody();
    }

    private void putDuraciones(boolean permitirOtras) {
        exchange(HttpMethod.PUT, "/api/v1/agenda/duraciones",
                Map.of("pasoMinutos", 30, "duraciones", List.of(60, 90, 120),
                        "duracionDefault", 90, "permitirOtrasDuraciones", permitirOtras),
                ownerHeaders(), String.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    void precioGeneralPisaElDeCadaCancha_yVuelveAPorCancha() {
        // GENERAL: todas las canchas exponen el mismo precio del complejo.
        exchange(HttpMethod.PUT, "/api/v1/agenda/precios",
                Map.of("precioModo", "GENERAL", "precioHoraGeneral", 9500),
                ownerHeaders(), String.class);
        List<Map<String, Object>> canchasGeneral = (List<Map<String, Object>>) publicConfig().get("canchas");
        assertThat(canchasGeneral).isNotEmpty();
        assertThat(canchasGeneral).allSatisfy(c ->
                assertThat(((Number) c.get("precioHora")).intValue()).isEqualTo(9500));

        // POR_CANCHA: vuelven a verse los precios propios (8000/7000/6000 del seed), distintos entre sí.
        exchange(HttpMethod.PUT, "/api/v1/agenda/precios",
                Map.of("precioModo", "POR_CANCHA"),
                ownerHeaders(), String.class);
        List<Map<String, Object>> canchasPorCancha = (List<Map<String, Object>>) publicConfig().get("canchas");
        assertThat(canchasPorCancha).extracting(c -> ((Number) c.get("precioHora")).intValue())
                .contains(8000, 7000, 6000);
    }

    @SuppressWarnings("unchecked")
    @Test
    void sinOtrasDuraciones_laPublicaSoloOfreceElTurnoPrincipal_yRechazaOtra() {
        try {
            putDuraciones(false);

            List<Integer> permitidas = (List<Integer>) publicConfig().get("duracionesPermitidas");
            assertThat(permitidas).containsExactly(90);
            assertThat(publicConfig().get("permitirOtrasDuraciones")).isEqualTo(false);

            // Una reserva de 60' (no es el turno principal) debe rechazarse.
            String fecha = LocalDate.now().plusDays(6).toString();
            ResponseEntity<String> resp = exchange(HttpMethod.POST, "/public/reservas",
                    Map.of("complejoId", 1, "fecha", fecha, "hora", "09:30", "duracion", 60,
                            "clienteNombre", "Tester60", "clienteWhatsapp", "5493510008888"),
                    publicHeaders(), String.class);
            assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
        } finally {
            // Restaurar el estado demo para no romper las otras clases IT que comparten la DB.
            putDuraciones(true);
        }
        assertThat((List<Integer>) publicConfig().get("duracionesPermitidas"))
                .containsExactly(60, 90, 120);
    }
}
