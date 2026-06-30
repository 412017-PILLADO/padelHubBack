package org.example.padelback.modules.reservas.application;

import java.math.BigDecimal;

import org.example.padelback.modules.reservas.domain.model.config.AgendaConfig;
import org.example.padelback.modules.reservas.domain.port.AgendaConfigCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Alta/edición/baja de canchas desde el panel. La baja es soft-delete: la cancha desaparece de la
 * disponibilidad nueva pero las reservas ya creadas sobre ella se conservan.
 */
@Service
@RequiredArgsConstructor
public class GestionCanchasUseCase {

    private final AgendaConfigCommandPort commandPort;

    public AgendaConfig.CanchaItem crear(Long complejoId, String nombre, Integer orden, boolean techada,
                                         String tipoPared, BigDecimal precioHora, String color) {
        return commandPort.crearCancha(complejoId, nombre, orden, techada, tipoPared, precioHora, color);
    }

    public AgendaConfig.CanchaItem actualizar(Long id, String nombre, Integer orden, boolean techada,
                                              String tipoPared, BigDecimal precioHora, String color, String estado) {
        return commandPort.actualizarCancha(id, nombre, orden, techada, tipoPared, precioHora, color, estado);
    }

    public void eliminar(Long id) {
        commandPort.eliminarCancha(id);
    }
}
