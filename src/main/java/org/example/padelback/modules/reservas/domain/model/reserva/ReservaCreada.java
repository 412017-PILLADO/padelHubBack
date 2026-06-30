package org.example.padelback.modules.reservas.domain.model.reserva;

import java.time.LocalDateTime;

public record ReservaCreada(
        Long id,
        Long canchaId,
        String canchaNombre,
        LocalDateTime inicio,
        LocalDateTime fin,
        int duracionMinutos) {}
