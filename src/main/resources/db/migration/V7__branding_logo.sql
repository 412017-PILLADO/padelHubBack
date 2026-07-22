-- =====================================================================
-- Logo del tenant guardado en la base (bytes), en tabla aparte para no cargar el blob en cada
-- /public/config. El branding textual (color_primario, fuente, logo_url) ya vive en `tenants`.
-- Se accede con JdbcTemplate, sin entidad JPA, así ddl-auto=validate no valida el tipo del blob.
-- =====================================================================
CREATE TABLE tenant_logos (
    tenant_id    BIGINT       NOT NULL PRIMARY KEY,
    bytes        LONGBLOB     NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    CONSTRAINT fk_tenant_logos_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);
