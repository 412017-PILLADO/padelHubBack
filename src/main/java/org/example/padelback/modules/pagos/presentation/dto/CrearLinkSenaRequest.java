package org.example.padelback.modules.pagos.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record CrearLinkSenaRequest(@NotNull Long reservaId, String backUrl) {}
