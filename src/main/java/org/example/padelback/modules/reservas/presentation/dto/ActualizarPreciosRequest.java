package org.example.padelback.modules.reservas.presentation.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;

/**
 * @param precioModo        "GENERAL" o "POR_CANCHA"
 * @param precioHoraGeneral precio por hora del complejo cuando el modo es GENERAL (null si POR_CANCHA)
 */
public record ActualizarPreciosRequest(
        @NotBlank String precioModo,
        BigDecimal precioHoraGeneral) {}
