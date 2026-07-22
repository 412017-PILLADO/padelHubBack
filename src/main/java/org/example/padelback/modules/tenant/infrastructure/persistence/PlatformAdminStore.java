package org.example.padelback.modules.tenant.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Acceso a {@code platform_admins} (super-admins) con JdbcTemplate, sin entidad JPA: estos usuarios
 * no son tenant-scoped, así que evitamos el filtro Hibernate de tenant. Espeja el enfoque de
 * {@code TenantLogoStore}.
 */
@Component
@RequiredArgsConstructor
public class PlatformAdminStore {

    private final JdbcTemplate jdbc;

    /** Vista de un super-admin para el login. */
    public record Admin(long id, String email, String passwordHash, String estado) {}

    public Optional<Admin> findActivoByEmail(String email) {
        try {
            Admin a = jdbc.queryForObject(
                    "SELECT id, email, password_hash, estado FROM platform_admins "
                            + "WHERE email = ? AND estado = 'ACTIVO'",
                    (rs, i) -> new Admin(rs.getLong("id"), rs.getString("email"),
                            rs.getString("password_hash"), rs.getString("estado")),
                    email);
            return Optional.ofNullable(a);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public boolean existsByEmail(String email) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM platform_admins WHERE email = ?", Integer.class, email);
        return n != null && n > 0;
    }

    /** Crea un super-admin (idempotencia la maneja el caller vía {@link #existsByEmail}). */
    public void create(String email, String passwordHash) {
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("INSERT INTO platform_admins (email, password_hash, estado, created_at, updated_at) "
                + "VALUES (?,?,?,?,?)", email, passwordHash, "ACTIVO", now, now);
    }
}
