-- Credenciales OAuth de Mercado Pago por tenant (tokens cifrados AES-GCM por la app).
-- Sin entidad JPA (acceso por JdbcTemplate, patrón tenant_logos): ddl-auto=validate no la valida.
CREATE TABLE tenant_mercadopago (
    tenant_id        BIGINT       NOT NULL PRIMARY KEY,
    mp_user_id       VARCHAR(32)  NOT NULL,
    mp_public_key    VARCHAR(128) NULL,
    access_token_cif VARCHAR(1024) NOT NULL,
    refresh_token_cif VARCHAR(1024) NULL,
    scope            VARCHAR(255) NULL,
    expira_en        DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    CONSTRAINT fk_tenant_mp_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Un pago de seña por reserva: preferencia creada y (si llegó) el pago que la saldó.
-- estado: PENDIENTE | APROBADO | APROBADO_TARDE | RECHAZADO
CREATE TABLE sena_pagos (
    reserva_id    BIGINT        NOT NULL PRIMARY KEY,
    tenant_id     BIGINT        NOT NULL,
    preference_id VARCHAR(64)   NOT NULL,
    init_point    VARCHAR(512)  NOT NULL,
    payment_id    VARCHAR(32)   NULL,
    estado        VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE',
    monto         DECIMAL(10,2) NOT NULL,
    updated_at    DATETIME(6)   NOT NULL,
    KEY idx_sena_pagos_tenant (tenant_id),
    KEY idx_sena_pagos_payment (payment_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
