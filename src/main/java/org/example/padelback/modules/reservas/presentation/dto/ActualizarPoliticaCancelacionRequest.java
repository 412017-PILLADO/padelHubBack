package org.example.padelback.modules.reservas.presentation.dto;

/**
 * @param texto texto libre de la política de cancelación/devolución (vacío o null = borra la política)
 */
public record ActualizarPoliticaCancelacionRequest(String texto) {}
