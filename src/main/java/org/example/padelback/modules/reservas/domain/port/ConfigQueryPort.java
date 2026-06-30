package org.example.padelback.modules.reservas.domain.port;

import java.util.List;
import java.util.Optional;

import org.example.padelback.modules.reservas.domain.model.config.ConfigPublico;

public interface ConfigQueryPort {

    List<Long> complejosActivos();

    Optional<ConfigPublico> cargar(Long complejoId);
}
