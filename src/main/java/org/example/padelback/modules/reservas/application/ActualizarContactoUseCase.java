package org.example.padelback.modules.reservas.application;

import org.example.padelback.modules.reservas.domain.model.config.AgendaConfig;
import org.example.padelback.modules.reservas.domain.port.AgendaConfigCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActualizarContactoUseCase {

    private final AgendaConfigCommandPort commandPort;

    public void ejecutar(AgendaConfig.Contacto contacto) {
        commandPort.actualizarContacto(contacto);
    }
}
