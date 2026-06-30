package org.example.padelback.modules.reservas.presentation.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

public record ActualizarDuracionesRequest(
        @Min(5) @Max(120) int pasoMinutos,
        @NotEmpty List<Integer> duraciones,
        int duracionDefault) {}
