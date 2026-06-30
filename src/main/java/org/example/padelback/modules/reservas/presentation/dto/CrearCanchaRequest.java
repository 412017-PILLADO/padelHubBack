package org.example.padelback.modules.reservas.presentation.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;

/**
 * Alta de cancha desde el panel. {@code complejoId} opcional: si falta se usa el único complejo
 * activo del tenant. {@code orden} opcional: si falta se ubica al final.
 */
public record CrearCanchaRequest(
        Long complejoId,
        @NotBlank String nombre,
        Integer orden,
        boolean techada,
        @NotBlank String tipoPared,
        BigDecimal precioHora,
        String color) {}
