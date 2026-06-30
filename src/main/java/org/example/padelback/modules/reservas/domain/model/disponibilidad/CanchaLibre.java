package org.example.padelback.modules.reservas.domain.model.disponibilidad;

import java.math.BigDecimal;

import org.example.padelback.modules.reservas.domain.model.TipoPared;

/** Cancha libre en un slot puntual. */
public record CanchaLibre(Long id, String nombre, String color, boolean techada,
                          TipoPared tipoPared, BigDecimal precioHora) {}
