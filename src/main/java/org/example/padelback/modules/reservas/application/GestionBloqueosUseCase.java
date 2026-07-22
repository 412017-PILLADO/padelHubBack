package org.example.padelback.modules.reservas.application;

import java.time.LocalDate;
import java.util.List;

import org.example.padelback.modules.reservas.domain.model.config.AgendaConfig;
import org.example.padelback.modules.reservas.domain.port.AgendaConfigCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GestionBloqueosUseCase {

    private final AgendaConfigCommandPort commandPort;

    /** @return reservas activas futuras que el bloqueo recién creado dejó solapadas (advertencia, no lo impide). */
    public List<AgendaConfig.ReservaAfectada> crear(LocalDate fecha, Long canchaId, String motivo) {
        return commandPort.crearBloqueo(fecha, canchaId, motivo);
    }

    public void eliminar(Long id) {
        commandPort.eliminarBloqueo(id);
    }
}
