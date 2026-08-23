package org.example.padelback.modules.auth.domain.model;

/** El tenant ya no viene de afuera: sale de la búsqueda por email, que es global. */
public record UsuarioAuth(Long id, Long tenantId, String email, String passwordHash, UsuarioRol rol) {}
