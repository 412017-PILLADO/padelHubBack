package org.example.padelback.modules.reservas.application;

import org.example.padelback.modules.reservas.domain.port.AgendaConfigCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActualizarAutoasignacionUseCase {

    private final AgendaConfigCommandPort commandPort;

    /**
     * @param autoasignacion si true, el sistema asigna la cancha automáticamente y la landing oculta
     *                       el paso de elegir cancha
     */
    public void ejecutar(boolean autoasignacion) {
        commandPort.actualizarAutoasignacion(autoasignacion);
    }
}
