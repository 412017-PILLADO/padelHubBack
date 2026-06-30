package org.example.padelback.modules.reservas.presentation.dto;

public record TurnoResponse(Long id, String hora, String fin, String clienteNombre,
                            String clienteWhatsapp, String canchaNombre, int duracionMinutos, String estado) {}
