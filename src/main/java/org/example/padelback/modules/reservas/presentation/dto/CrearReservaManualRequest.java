package org.example.padelback.modules.reservas.presentation.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

/** Body de `POST /api/v1/turnos`: reserva cargada a mano por el dueño desde el panel. */
public record CrearReservaManualRequest(
        Long complejoId,
        /** null = "cualquiera disponible" (el back asigna la cancha menos cargada). */
        Long canchaId,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime hora,
        Integer duracion,
        @NotBlank String clienteNombre,
        /** Opcional: el dueño puede no tener el WhatsApp del cliente a mano. */
        String clienteWhatsapp) {}
