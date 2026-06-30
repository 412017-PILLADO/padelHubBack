package org.example.padelback.modules.reservas.presentation.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

public record CrearBloqueoRequest(
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
        Long canchaId,
        String motivo) {}
