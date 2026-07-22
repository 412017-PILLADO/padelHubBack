-- =====================================================================
-- Admins de plataforma (super-admin): usuarios que NO pertenecen a un tenant y dan de alta clubes.
-- Tabla aparte de `usuarios` (que es tenant-scoped por el filtro Hibernate) — se accede con
-- JdbcTemplate, sin entidad JPA, para no arrastrar el scope de tenant ni el auditing.
-- =====================================================================
CREATE TABLE platform_admins (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    estado        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL
);
