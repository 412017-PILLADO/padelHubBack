package org.example.padelback.modules.reservas.application;

import java.math.BigDecimal;

import org.example.padelback.modules.reservas.domain.port.AgendaConfigCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActualizarSenaUseCase {

    private final AgendaConfigCommandPort commandPort;

    /**
     * @param requiereSena activa/desactiva el módulo de señas del complejo
     * @param senaMonto    monto informativo de la seña (obligatorio y &gt; 0 si el módulo está activo)
     * @param senaAlias    alias/CBU de transferencia (obligatorio si el módulo está activo)
     */
    public void ejecutar(boolean requiereSena, BigDecimal senaMonto, String senaAlias) {
        commandPort.actualizarSena(requiereSena, senaMonto, senaAlias);
    }
}
