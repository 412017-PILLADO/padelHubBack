package org.example.padelback.modules.reservas.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

/** Replace-all de las franjas de precio especial del complejo (lista vacía = sin franjas). */
public record GuardarPrecioFranjasRequest(@NotNull List<@Valid FranjaRequest> franjas) {

    public record FranjaRequest(
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime desde,
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime hasta,
            BigDecimal precioHora) {}
}
