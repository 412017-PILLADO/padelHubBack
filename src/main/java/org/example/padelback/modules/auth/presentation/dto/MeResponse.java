package org.example.padelback.modules.auth.presentation.dto;

public record MeResponse(String email, Long tenantId, String tenantName, String rol) {}
