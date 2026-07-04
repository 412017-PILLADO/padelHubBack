package org.example.padelback.modules.reservas.application;

import java.math.BigDecimal;

import org.example.padelback.modules.reservas.domain.port.AgendaConfigCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActualizarPreciosUseCase {

    private final AgendaConfigCommandPort commandPort;

    /**
     * @param precioModo         GENERAL o POR_CANCHA
     * @param precioHoraGeneral  precio por hora del complejo (obligatorio si modo GENERAL)
     */
    public void ejecutar(String precioModo, BigDecimal precioHoraGeneral) {
        commandPort.actualizarPrecios(precioModo, precioHoraGeneral);
    }
}
