package org.example.padelback.modules.reservas.application;

import java.util.List;

import org.example.padelback.modules.reservas.domain.exception.ComplejoNoResueltoException;
import org.example.padelback.modules.reservas.domain.model.config.ConfigPublico;
import org.example.padelback.modules.reservas.domain.port.ConfigQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConsultarConfigUseCase {

    private final ConfigQueryPort configQueryPort;

    public ConfigPublico ejecutar(Long complejoId) {
        Long resuelto = resolverComplejo(complejoId);
        return configQueryPort.cargar(resuelto)
                .orElseThrow(() -> new ComplejoNoResueltoException("Complejo " + resuelto + " no encontrado"));
    }

    private Long resolverComplejo(Long complejoId) {
        if (complejoId != null) {
            return complejoId;
        }
        List<Long> activos = configQueryPort.complejosActivos();
        if (activos.size() != 1) {
            throw new ComplejoNoResueltoException(
                    "Se requiere complejoId: el tenant tiene " + activos.size() + " complejos activos");
        }
        return activos.get(0);
    }
}
