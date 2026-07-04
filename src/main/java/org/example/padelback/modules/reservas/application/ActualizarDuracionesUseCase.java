package org.example.padelback.modules.reservas.application;

import java.util.List;

import org.example.padelback.modules.reservas.domain.port.AgendaConfigCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActualizarDuracionesUseCase {

    private final AgendaConfigCommandPort commandPort;

    public void ejecutar(int pasoMinutos, List<Integer> duraciones, int duracionDefault,
                         boolean permitirOtrasDuraciones) {
        commandPort.actualizarDuraciones(pasoMinutos, duraciones, duracionDefault, permitirOtrasDuraciones);
    }
}
