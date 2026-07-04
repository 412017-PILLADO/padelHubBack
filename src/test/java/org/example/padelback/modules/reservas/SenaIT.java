package org.example.padelback.modules.reservas;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.example.padelback.modules.reservas.domain.model.ReservaEstado;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.ReservaJpaRepository;
import org.example.padelback.modules.reservas.infrastructure.scheduling.SenaCleanupJob;
import org.example.padelback.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Módulo de señas: con el complejo pidiendo seña, la reserva nace PENDIENTE, retiene la cancha, y el
 * dueño la confirma o rechaza desde el panel. Cubre también la expiración perezosa (una PENDIENTE
 * vencida deja de bloquear el slot al instante) y el job que la pasa a CANCELADO.
 *
 * <p>El módulo es config global del complejo (compartida por la DB singleton), así que cada test lo
 * activa al empezar y lo apaga en un finally para no contaminar a los demás.
 */
class SenaIT extends IntegrationTestBase {

    @Autowired
    ReservaJpaRepository reservaRepo;
    @Autowired
    SenaCleanupJob senaCleanupJob;
    @Autowired
    JdbcTemplate jdbc;

    private void setSena(boolean requiere, Integer monto) {
        Map<String, Object> body = new HashMap<>();
        body.put("requiereSena", requiere);
        body.put("senaMonto", monto);
        // Con el módulo activo el alias es obligatorio (es a dónde transfiere el cliente).
        body.put("senaAlias", requiere ? "padel.hub.test" : null);
        ResponseEntity<String> resp = exchange(HttpMethod.PUT, "/api/v1/agenda/sena", body, ownerHeaders(), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private Map<String, Object> reservaBody(String fecha, String hora, String tel) {
        Map<String, Object> body = new HashMap<>();
        body.put("complejoId", 1);
        body.put("canchaId", 1);
        body.put("fecha", fecha);
        body.put("hora", hora);
        body.put("duracion", 90);
        body.put("clienteNombre", "Tester Seña");
        body.put("clienteWhatsapp", tel);
        return body;
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> reservar(String fecha, String hora, String tel) {
        return exchange(HttpMethod.POST, "/public/reservas", reservaBody(fecha, hora, tel), publicHeaders(), Map.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    void senaActivada_reservaNacePendiente_bloqueaSlot_yConfirmarLaConfirma() {
        String fecha = LocalDate.now().plusDays(20).toString();
        try {
            setSena(true, 5000);

            // La config pública ahora avisa que se pide seña, con el monto.
            ResponseEntity<Map> config = exchange(HttpMethod.GET, "/public/config", null, publicHeaders(), Map.class);
            assertThat(config.getBody().get("requiereSena")).isEqualTo(true);
            assertThat(((Number) config.getBody().get("senaMonto")).intValue()).isEqualTo(5000);
            assertThat(config.getBody().get("senaAlias")).isEqualTo("padel.hub.test");

            ResponseEntity<Map> creada = reservar(fecha, "09:30", "5493511110020");
            assertThat(creada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(creada.getBody().get("estado")).isEqualTo("PENDIENTE");
            long id = ((Number) creada.getBody().get("id")).longValue();

            // El pendiente retiene la cancha: otra reserva del mismo slot y cancha choca (409).
            ResponseEntity<String> choque = exchange(HttpMethod.POST, "/public/reservas",
                    reservaBody(fecha, "09:30", "5493511110021"), publicHeaders(), String.class);
            assertThat(choque.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

            // Aparece en la cola de pendientes del panel.
            ResponseEntity<List> pendientes = exchange(HttpMethod.GET, "/api/v1/turnos/pendientes",
                    null, ownerHeaders(), List.class);
            assertThat(pendientes.getBody()).anyMatch(p -> ((Number) ((Map<String, Object>) p).get("id")).longValue() == id);

            // El dueño confirma la seña → CONFIRMADO y sale de la cola.
            ResponseEntity<Map> confirmar = exchange(HttpMethod.POST, "/api/v1/turnos/" + id + "/confirmar-sena",
                    null, ownerHeaders(), Map.class);
            assertThat(confirmar.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(confirmar.getBody().get("estado")).isEqualTo("CONFIRMADO");

            ResponseEntity<List> pendientes2 = exchange(HttpMethod.GET, "/api/v1/turnos/pendientes",
                    null, ownerHeaders(), List.class);
            assertThat(pendientes2.getBody()).noneMatch(p -> ((Number) ((Map<String, Object>) p).get("id")).longValue() == id);
        } finally {
            setSena(false, null);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void rechazarSena_liberaElSlot() {
        String fecha = LocalDate.now().plusDays(21).toString();
        try {
            setSena(true, 4000);

            ResponseEntity<Map> creada = reservar(fecha, "11:00", "5493511110022");
            assertThat(creada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            long id = ((Number) creada.getBody().get("id")).longValue();

            ResponseEntity<Map> rechazo = exchange(HttpMethod.POST, "/api/v1/turnos/" + id + "/rechazar-sena",
                    null, ownerHeaders(), Map.class);
            assertThat(rechazo.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(rechazo.getBody().get("estado")).isEqualTo("CANCELADO");

            // Con el rechazo el slot quedó libre: se puede volver a reservar.
            ResponseEntity<Map> reintento = reservar(fecha, "11:00", "5493511110023");
            assertThat(reintento.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        } finally {
            setSena(false, null);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void pendienteVencida_liberaSlotAlInstante_yJobLaCancela() {
        String fecha = LocalDate.now().plusDays(22).toString();
        try {
            setSena(true, 6000);

            ResponseEntity<Map> creada = reservar(fecha, "12:30", "5493511110024");
            assertThat(creada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            long id = ((Number) creada.getBody().get("id")).longValue();

            // Forzamos el vencimiento: expira_en al pasado.
            jdbc.update("UPDATE reservas SET expira_en = '2000-01-01 00:00:00' WHERE id = ?", id);

            // Expiración perezosa: el slot ya no está retenido → se puede reservar de nuevo (201).
            ResponseEntity<Map> reintento = reservar(fecha, "12:30", "5493511110025");
            assertThat(reintento.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            // El job pasa la vencida a CANCELADO (limpieza cosmética).
            senaCleanupJob.cancelarSenasVencidas();
            assertThat(reservaRepo.findByTenantIdAndIdAndActiveTrue(1L, id))
                    .get().extracting("estado").isEqualTo(ReservaEstado.CANCELADO);
        } finally {
            setSena(false, null);
        }
    }
}
