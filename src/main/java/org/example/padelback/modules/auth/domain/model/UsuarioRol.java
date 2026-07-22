package org.example.padelback.modules.auth.domain.model;

public enum UsuarioRol {
    OWNER,
    STAFF,
    /** Admin de plataforma (super-admin): no pertenece a ningún tenant; gestiona el alta de clubes. */
    SUPERADMIN
}
