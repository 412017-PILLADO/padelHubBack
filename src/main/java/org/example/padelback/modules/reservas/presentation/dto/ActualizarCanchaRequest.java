package org.example.padelback.modules.reservas.presentation.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;

/** Edición de cancha. {@code estado} = ACTIVO/INACTIVO; null se interpreta como ACTIVO. */
public record ActualizarCanchaRequest(
        @NotBlank String nombre,
        Integer orden,
        boolean techada,
        @NotBlank String tipoPared,
        BigDecimal precioHora,
        String color,
        String estado) {}
