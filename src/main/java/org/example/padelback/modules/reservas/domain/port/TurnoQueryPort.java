package org.example.padelback.modules.reservas.domain.port;

import java.time.LocalDate;
import java.util.List;

import org.example.padelback.modules.reservas.domain.model.turno.PendienteDeSena;
import org.example.padelback.modules.reservas.domain.model.turno.TurnoDelDia;

public interface TurnoQueryPort {
    List<TurnoDelDia> turnosDelDia(LocalDate fecha);

    /** Reservas PENDIENTE de seña todavía vigentes (todas las fechas), más urgentes primero. */
    List<PendienteDeSena> pendientesDeSena();
}
