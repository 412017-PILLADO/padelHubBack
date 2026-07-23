package org.example.padelback.modules.reservas.presentation.dto;

/** Estado actual de la marca del tenant para el panel. */
public record MarcaResponse(String colorPrimario, String colorSecundario, String plantilla,
                            String fuente, String logoUrl) {}
