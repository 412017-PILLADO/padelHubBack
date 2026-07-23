package org.example.padelback.modules.reservas.domain.model.disponibilidad;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Franja horaria con precio especial GENERAL del complejo (pisa el precio habitual de todas las
 * canchas por igual, en ambos modos de precio, y aplica todos los días). Un turno paga
 * {@code precioHora} si su hora de inicio cae dentro de {@code [desde, hasta)}, con {@code hasta}
 * = 00:00 interpretado como 24:00 (mismo criterio que {@link Franja}).
 */
public record PrecioFranja(LocalTime desde, LocalTime hasta, BigDecimal precioHora) {}
