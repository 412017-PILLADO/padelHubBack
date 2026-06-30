package org.example.padelback.modules.reservas.presentation.dto;

import java.math.BigDecimal;

public record CanchaLibreResponse(Long id, String nombre, String color, boolean techada,
                                  String tipoPared, BigDecimal precioHora) {}
