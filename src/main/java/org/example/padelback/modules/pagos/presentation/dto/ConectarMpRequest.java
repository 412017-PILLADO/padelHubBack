package org.example.padelback.modules.pagos.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record ConectarMpRequest(@NotBlank String returnTo) {}
