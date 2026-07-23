package org.example.padelback.modules.reservas.domain.model.disponibilidad;

import java.time.LocalTime;

/**
 * Franja horaria con ajuste porcentual de precio, GENERAL del complejo: se aplica sobre el precio
 * original de cada cancha (o el general), así todas mantienen su precio relativo. Negativo =
 * descuento (-20 → paga 80%), positivo = recargo (+15 → paga 115%). Aplica todos los días. Un turno
 * la paga si su hora de inicio cae dentro de {@code [desde, hasta)}, con {@code hasta} = 00:00
 * interpretado como 24:00 (mismo criterio que {@link Franja}).
 */
public record PrecioFranja(LocalTime desde, LocalTime hasta, int ajustePorcentaje) {}
