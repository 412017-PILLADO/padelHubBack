package org.example.padelback.modules.reservas.application;

import org.example.padelback.modules.reservas.domain.port.TurnoCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Validación manual de la seña desde el panel: el dueño confirma (vio el pago) o rechaza la reserva. */
@Service
@RequiredArgsConstructor
public class ValidarSenaUseCase {

    private final TurnoCommandPort turnoCommandPort;

    @Transactional
    public void confirmar(Long turnoId) {
        turnoCommandPort.confirmarSena(turnoId);
    }

    @Transactional
    public void rechazar(Long turnoId) {
        turnoCommandPort.rechazarSena(turnoId);
    }
}
