package org.example.padelback.modules.reservas.presentation.dto;

import java.time.LocalDate;

public record ArrepentimientoResponse(
        Long id, String codigo, String nombre, String whatsapp, String detalle,
        LocalDate reservaFecha, boolean gestionado, String creado) {}
