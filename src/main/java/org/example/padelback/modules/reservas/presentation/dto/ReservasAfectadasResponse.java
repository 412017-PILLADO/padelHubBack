package org.example.padelback.modules.reservas.presentation.dto;

import java.util.List;

/**
 * Respuesta de {@code PUT /agenda/horarios} y {@code POST /agenda/bloqueos}: el cambio se guarda
 * igual (no es un rechazo), pero si dejó reservas CONFIRMADO/PENDIENTE-vigente futuras fuera de la
 * nueva franja (o solapadas con el bloqueo recién creado), se listan acá para que el panel avise al
 * dueño. Array vacío = sin impacto.
 */
public record ReservasAfectadasResponse(List<ReservaAfectadaResponse> reservasAfectadas) {

    public record ReservaAfectadaResponse(Long id, String fecha, String hora, String cancha, String cliente) {}
}
