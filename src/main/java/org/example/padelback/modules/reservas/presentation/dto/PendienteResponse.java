package org.example.padelback.modules.reservas.presentation.dto;

/**
 * Una reserva pendiente de seña para el panel. Lleva {@code fecha} (los pendientes cruzan días) y
 * {@code expiraEn} (ISO, para que el panel muestre el tiempo restante).
 */
public record PendienteResponse(Long id, String fecha, String hora, String fin, String clienteNombre,
                                String clienteWhatsapp, String canchaNombre, int duracionMinutos,
                                String expiraEn) {}
