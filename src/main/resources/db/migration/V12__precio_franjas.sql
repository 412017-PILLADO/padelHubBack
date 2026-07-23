-- =====================================================================
-- PrecioFranja (franja horaria con precio especial, GENERAL del complejo: pisa el precio
-- habitual de TODAS las canchas por igual, tanto en modo GENERAL como POR_CANCHA, y aplica
-- todos los días por igual). Un turno paga esta tarifa si su hora de inicio cae dentro de
-- [hora_desde, hora_hasta) — hora_hasta = 00:00 se interpreta como 24:00, igual que en
-- horarios_complejo.
-- =====================================================================
CREATE TABLE precio_franjas (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    tenant_id   BIGINT        NOT NULL,
    complejo_id BIGINT        NOT NULL,
    hora_desde  TIME          NOT NULL,
    hora_hasta  TIME          NOT NULL,
    precio_hora DECIMAL(10,2) NOT NULL,
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    deleted_at  TIMESTAMP(6)  NULL,
    created_at  TIMESTAMP(6)  NOT NULL,
    updated_at  TIMESTAMP(6)  NOT NULL,
    created_by  VARCHAR(120)  NOT NULL,
    updated_by  VARCHAR(120)  NOT NULL,
    version     BIGINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_precio_franjas_tenant_id (tenant_id),
    INDEX idx_precio_franjas_complejo (tenant_id, complejo_id),
    CONSTRAINT fk_precio_franjas_complejo FOREIGN KEY (complejo_id) REFERENCES complejos (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
