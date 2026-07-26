package org.example.padelback.modules.reservas;

import java.util.List;
import java.util.Map;

import org.example.padelback.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArrepentimientoIT extends IntegrationTestBase {

    @Test
    @SuppressWarnings("unchecked")
    void flujoCompletoDeArrepentimiento() {
        // 1. público, sin registro → código inmediato
        ResponseEntity<Map> alta = exchange(HttpMethod.POST, "/public/arrepentimiento",
                Map.of("nombre", "Carla Prueba", "whatsapp", "3511234567",
                        "detalle", "Quiero cancelar la reserva del sábado", "empresa", ""),
                publicHeaders(), Map.class);
        assertEquals(201, alta.getStatusCode().value());
        String codigo = (String) alta.getBody().get("codigo");
        assertTrue(codigo.matches("ARR-[A-Z0-9]{6}"));

        // 2. honeypot lleno → 400 silencioso
        assertEquals(400, exchange(HttpMethod.POST, "/public/arrepentimiento",
                Map.of("nombre", "Bot", "whatsapp", "111", "empresa", "spam"),
                publicHeaders(), Map.class).getStatusCode().value());

        // 3. el panel lo ve pendiente
        ResponseEntity<List> lista = exchange(HttpMethod.GET, "/api/v1/arrepentimientos",
                null, ownerHeaders(), List.class);
        Map<String, Object> item = (Map<String, Object>) lista.getBody().stream()
                .filter(a -> codigo.equals(((Map<?, ?>) a).get("codigo"))).findFirst().orElseThrow();
        assertEquals(Boolean.FALSE, item.get("gestionado"));
        Number id = (Number) item.get("id");

        // 4. gestionar
        assertEquals(204, exchange(HttpMethod.POST, "/api/v1/arrepentimientos/" + id + "/gestionar",
                null, ownerHeaders(), Void.class).getStatusCode().value());
        ResponseEntity<List> lista2 = exchange(HttpMethod.GET, "/api/v1/arrepentimientos",
                null, ownerHeaders(), List.class);
        Map<String, Object> item2 = (Map<String, Object>) lista2.getBody().stream()
                .filter(a -> codigo.equals(((Map<?, ?>) a).get("codigo"))).findFirst().orElseThrow();
        assertEquals(Boolean.TRUE, item2.get("gestionado"));
    }
}
