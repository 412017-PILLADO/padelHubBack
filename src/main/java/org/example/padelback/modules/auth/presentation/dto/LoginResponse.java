package org.example.padelback.modules.auth.presentation.dto;

public record LoginResponse(String token, long expiresIn) {}
