package org.example.padelback.modules.reservas.domain.model.disponibilidad;

import java.math.BigDecimal;

import org.example.padelback.modules.reservas.domain.model.TipoPared;

/** Datos de una cancha que se exponen al construir la disponibilidad. */
public record CanchaRef(Long id, String nombre, String color, boolean techada,
                        TipoPared tipoPared, BigDecimal precioHora) {}
