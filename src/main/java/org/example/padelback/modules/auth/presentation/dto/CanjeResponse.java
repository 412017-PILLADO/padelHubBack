package org.example.padelback.modules.auth.presentation.dto;

/** Lo que antes devolvía el login: el JWT y cuánto dura. */
public record CanjeResponse(String token, long expiresIn) {}
