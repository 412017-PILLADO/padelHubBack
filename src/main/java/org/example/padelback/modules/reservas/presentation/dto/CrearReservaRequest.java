package org.example.padelback.modules.reservas.presentation.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

public record CrearReservaRequest(
        Long complejoId,
        /** null = "cualquiera disponible" (el back asigna la cancha menos cargada). */
        Long canchaId,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime hora,
        @NotNull Integer duracion,
        @NotBlank String clienteNombre,
        String clienteWhatsapp,
        /** Honeypot anti-bot: si viene con texto, se descarta la solicitud. */
        String empresa) {}
