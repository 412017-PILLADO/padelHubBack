package org.example.padelback.modules.reservas.domain.model.turno;

import java.time.LocalDateTime;

/**
 * Una reserva PENDIENTE de seña tal como la ve el panel para validarla: además de los datos del turno,
 * lleva {@code expiraEn} para mostrar cuánto tiempo queda antes de que la reserva se caiga sola.
 */
public record PendienteDeSena(
        Long id, LocalDateTime inicio, LocalDateTime fin, String clienteNombre, String clienteWhatsapp,
        String canchaNombre, int duracionMinutos, LocalDateTime expiraEn) {}
