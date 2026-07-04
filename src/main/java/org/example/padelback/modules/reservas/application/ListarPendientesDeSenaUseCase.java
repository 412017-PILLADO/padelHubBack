package org.example.padelback.modules.reservas.application;

import java.util.List;

import org.example.padelback.modules.reservas.domain.model.turno.PendienteDeSena;
import org.example.padelback.modules.reservas.domain.port.TurnoQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListarPendientesDeSenaUseCase {

    private final TurnoQueryPort turnoQueryPort;

    public List<PendienteDeSena> ejecutar() {
        return turnoQueryPort.pendientesDeSena();
    }
}
