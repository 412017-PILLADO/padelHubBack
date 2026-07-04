package org.example.padelback.modules.reservas.presentation.dto;

/**
 * @param autoasignacion si true, el sistema asigna una cancha disponible automáticamente y la landing
 *                       oculta el paso de elegir cancha
 */
public record ActualizarAutoasignacionRequest(boolean autoasignacion) {}
