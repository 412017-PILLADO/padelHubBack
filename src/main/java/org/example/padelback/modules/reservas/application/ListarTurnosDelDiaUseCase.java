package org.example.padelback.modules.reservas.application;

import java.time.LocalDate;
import java.util.List;

import org.example.padelback.modules.reservas.domain.model.turno.TurnoDelDia;
import org.example.padelback.modules.reservas.domain.port.TurnoQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListarTurnosDelDiaUseCase {

    private final TurnoQueryPort turnoQueryPort;

    public List<TurnoDelDia> ejecutar(LocalDate fecha) {
        return turnoQueryPort.turnosDelDia(fecha);
    }
}
