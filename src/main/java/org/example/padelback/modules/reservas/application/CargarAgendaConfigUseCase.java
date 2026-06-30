package org.example.padelback.modules.reservas.application;

import org.example.padelback.modules.reservas.domain.model.config.AgendaConfig;
import org.example.padelback.modules.reservas.domain.port.AgendaConfigQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CargarAgendaConfigUseCase {

    private final AgendaConfigQueryPort queryPort;

    public AgendaConfig ejecutar() {
        return queryPort.cargar();
    }
}
