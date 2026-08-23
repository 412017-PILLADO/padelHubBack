package org.example.padelback.modules.auth.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record CanjeRequest(@NotBlank String code) {}
