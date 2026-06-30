package org.example.padelback.modules.auth.domain.model;

public record UsuarioAuth(Long id, String email, String passwordHash, UsuarioRol rol) {}
