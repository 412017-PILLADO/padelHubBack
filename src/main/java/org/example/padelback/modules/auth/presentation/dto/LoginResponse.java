package org.example.padelback.modules.auth.presentation.dto;

/** Respuesta del login del apex: a qué club redirigir, y con qué código entrar. */
public record LoginResponse(String slug, String code) {}
