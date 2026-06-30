package org.example.padelback.modules.reservas.presentation.dto;

import java.time.LocalDateTime;

public record ReservaResponse(
        Long id,
        Long canchaId,
        String canchaNombre,
        LocalDateTime inicio,
        LocalDateTime fin,
        int duracionMinutos,
        String estado) {}
