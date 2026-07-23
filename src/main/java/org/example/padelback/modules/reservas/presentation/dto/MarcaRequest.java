package org.example.padelback.modules.reservas.presentation.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Actualización de la marca del tenant: color primario/secundario (hex) y fuente opcional. */
public record MarcaRequest(
        @Pattern(regexp = "^#([0-9a-fA-F]{6}|[0-9a-fA-F]{3})$",
                message = "El color debe ser un hex como #2747ff")
        String colorPrimario,
        @Pattern(regexp = "^#([0-9a-fA-F]{6}|[0-9a-fA-F]{3})$",
                message = "El color secundario debe ser un hex como #2747ff")
        String colorSecundario,
        @Pattern(regexp = "^[A-Ca-c]$", message = "La plantilla debe ser A, B o C")
        String plantilla,
        @Size(max = 80)
        String fuente) {}
