package org.example.padelback.modules.tenant.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Almacén del logo del tenant (bytes) en la tabla {@code tenant_logos}. Se usa JdbcTemplate en vez
 * de una entidad JPA para no meter un blob en el modelo (y que {@code ddl-auto=validate} no valide
 * el tipo LONGBLOB), y para no cargar los bytes salvo cuando se sirve el logo.
 */
@Component
@RequiredArgsConstructor
public class TenantLogoStore {

    private final JdbcTemplate jdbc;

    public record Logo(byte[] bytes, String contentType) {}

    @Transactional(readOnly = true)
    public boolean exists(long tenantId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tenant_logos WHERE tenant_id = ?", Integer.class, tenantId);
        return n != null && n > 0;
    }

    @Transactional(readOnly = true)
    public Optional<Logo> load(long tenantId) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    "SELECT bytes, content_type FROM tenant_logos WHERE tenant_id = ?",
                    (rs, i) -> new Logo(rs.getBytes("bytes"), rs.getString("content_type")),
                    tenantId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Transactional
    public void save(long tenantId, byte[] bytes, String contentType) {
        jdbc.update(
                "INSERT INTO tenant_logos (tenant_id, bytes, content_type, updated_at) VALUES (?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE bytes = VALUES(bytes), content_type = VALUES(content_type), "
                        + "updated_at = VALUES(updated_at)",
                tenantId, bytes, contentType, java.sql.Timestamp.from(Instant.now()));
    }

    @Transactional
    public void delete(long tenantId) {
        jdbc.update("DELETE FROM tenant_logos WHERE tenant_id = ?", tenantId);
    }
}
