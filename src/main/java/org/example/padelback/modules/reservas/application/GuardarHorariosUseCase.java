package org.example.padelback.modules.reservas.application;

import java.time.LocalTime;
import java.util.List;

import org.example.padelback.modules.reservas.domain.model.config.AgendaConfig;
import org.example.padelback.modules.reservas.domain.port.AgendaConfigCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GuardarHorariosUseCase {

    private final AgendaConfigCommandPort commandPort;

    /** @return reservas futuras que el cambio de horarios dejó fuera de franja (advertencia, no bloquea el guardado). */
    public List<AgendaConfig.ReservaAfectada> ejecutar(boolean breakOn, LocalTime breakFrom, LocalTime breakTo,
                         List<AgendaConfig.DiaConfig> week) {
        return commandPort.guardarHorarios(breakOn, breakFrom, breakTo, week);
    }
}
