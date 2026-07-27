package org.example.padelback.modules.reservas.application;

import org.example.padelback.modules.reservas.domain.port.AgendaConfigCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActualizarPoliticaCancelacionUseCase {

    private final AgendaConfigCommandPort commandPort;

    public void ejecutar(String texto) {
        commandPort.actualizarPoliticaCancelacion(texto);
    }
}
