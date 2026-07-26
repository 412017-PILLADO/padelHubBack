-- Solicitudes de arrepentimiento (Res. 424/2020): el consumidor revoca sin registro previo y
-- recibe un código; el dueño las gestiona desde el panel (cancela reserva / devuelve seña aparte).
-- Columnas de auditoría calcadas de `bloqueos` (V1__create_schema.sql) porque esta tabla SÍ tiene
-- entidad JPA (BaseJpaEntity) y ddl-auto=validate exige que coincidan exacto.
CREATE TABLE arrepentimientos (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id        BIGINT       NOT NULL,
    codigo           VARCHAR(12)  NOT NULL,
    nombre           VARCHAR(150) NOT NULL,
    whatsapp         VARCHAR(40)  NOT NULL,
    detalle          TEXT         NULL,
    reserva_fecha    DATE         NULL,
    gestionado       BOOLEAN      NOT NULL DEFAULT FALSE,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted_at       TIMESTAMP(6) NULL,
    created_at       TIMESTAMP(6) NOT NULL,
    updated_at       TIMESTAMP(6) NOT NULL,
    created_by       VARCHAR(120) NOT NULL,
    updated_by       VARCHAR(120) NOT NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_arrep_codigo (tenant_id, codigo),
    KEY idx_arrep_tenant_gestionado (tenant_id, gestionado)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
