package org.example.padelback.modules.reservas.presentation.dto;

import java.util.List;

public record SlotResponse(String hora, boolean disponible, List<CanchaLibreResponse> canchasLibres) {}
