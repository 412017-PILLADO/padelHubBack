package org.example.padelback.modules.reservas.application;

import org.example.padelback.modules.reservas.domain.port.TurnoCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CancelarTurnoUseCase {

    private final TurnoCommandPort turnoCommandPort;

    @Transactional
    public void ejecutar(Long turnoId) {
        turnoCommandPort.cancelar(turnoId);
    }
}
