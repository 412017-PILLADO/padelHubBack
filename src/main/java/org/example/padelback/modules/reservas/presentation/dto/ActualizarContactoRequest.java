package org.example.padelback.modules.reservas.presentation.dto;

import jakarta.validation.constraints.Size;

/** Datos de contacto editables desde el panel. Todos opcionales (se pueden dejar vacíos). */
public record ActualizarContactoRequest(
        @Size(max = 255) String direccion,
        @Size(max = 40) String telefono,
        @Size(max = 40) String whatsapp,
        @Size(max = 500) String mapaUrl,
        @Size(max = 100) String instagram) {}
