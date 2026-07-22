package org.example.padelback.modules.reservas.application;

import java.util.List;

import org.example.padelback.modules.reservas.domain.model.config.AgendaConfig;
import org.example.padelback.modules.reservas.domain.port.AgendaConfigCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GuardarPrecioFranjasUseCase {

    private final AgendaConfigCommandPort commandPort;

    /** Replace-all: reemplaza TODAS las franjas de precio del complejo (lista vacía = sin franjas). */
    public void ejecutar(List<AgendaConfig.PrecioFranjaItem> franjas) {
        commandPort.guardarPrecioFranjas(franjas);
    }
}
