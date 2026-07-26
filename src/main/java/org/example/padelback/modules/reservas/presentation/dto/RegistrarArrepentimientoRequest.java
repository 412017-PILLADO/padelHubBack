package org.example.padelback.modules.reservas.presentation.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;

public record RegistrarArrepentimientoRequest(
        @NotBlank String nombre,
        @NotBlank String whatsapp,
        String detalle,
        LocalDate reservaFecha,
        /** Honeypot anti-bot, mismo patrón que {@code CrearReservaRequest}. */
        String empresa) {}
