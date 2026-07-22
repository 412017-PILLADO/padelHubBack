package org.example.padelback.modules.reservas;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.example.padelback.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Anti-doble-reserva bajo concurrencia (C1): N hilos disparan a la vez la MISMA cancha/slot y solo
 * uno debe crear la reserva; el resto recibe 409. Verifica el combo lock pesimista + re-chequeo en
 * transacción nueva (snapshot fresco post-lock) + UNIQUE (tenant_id, slot_activo) de respaldo.
 *
 * <p>Requiere Docker (Testcontainers MySQL); no corre en entornos sin Docker, pero el assert es real.
 * Cada hilo usa un teléfono distinto para no toparse con el límite por teléfono (el objetivo acá es
 * la unicidad del slot, no el anti-abuso).
 */
class ConcurrenciaReservaIT extends IntegrationTestBase {

    private static final int HILOS = 8;

    private Map<String, Object> reservaBody(String fecha, String hora, Long canchaId, String tel) {
        Map<String, Object> body = new HashMap<>();
        body.put("complejoId", 1);
        body.put("canchaId", canchaId);
        body.put("fecha", fecha);
        body.put("hora", hora);
        body.put("duracion", 90);
        body.put("clienteNombre", "Tester");
        body.put("clienteWhatsapp", tel);
        return body;
    }

    @Test
    void ochoHilosMismoSlotCreanExactamenteUna() throws Exception {
        // Fecha lejana propia de esta clase: las clases IT comparten una sola DB (singleton container).
        String fecha = LocalDate.now().plusDays(40).toString();
        String hora = "09:30"; // inicio anclado válido de la grilla de 90'.

        ExecutorService pool = Executors.newFixedThreadPool(HILOS);
        CountDownLatch listos = new CountDownLatch(HILOS);
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger creadas = new AtomicInteger();
        AtomicInteger conflictos = new AtomicInteger();

        try {
            List<Future<?>> tareas = new java.util.ArrayList<>();
            for (int i = 0; i < HILOS; i++) {
                final String tel = "549351009" + String.format("%04d", i);
                tareas.add(pool.submit(() -> {
                    listos.countDown();
                    largada.await(); // todos arrancan el POST a la vez
                    ResponseEntity<String> resp = exchange(HttpMethod.POST, "/public/reservas",
                            reservaBody(fecha, hora, 1L, tel), publicHeaders(), String.class);
                    if (resp.getStatusCode() == HttpStatus.CREATED) {
                        creadas.incrementAndGet();
                    } else if (resp.getStatusCode() == HttpStatus.CONFLICT) {
                        conflictos.incrementAndGet();
                    }
                    return null;
                }));
            }

            listos.await(10, TimeUnit.SECONDS);
            largada.countDown();
            for (Future<?> t : tareas) {
                t.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        // Exactamente una reserva creada; el resto, conflicto (slot tomado).
        assertThat(creadas.get()).isEqualTo(1);
        assertThat(conflictos.get()).isEqualTo(HILOS - 1);
    }
}
