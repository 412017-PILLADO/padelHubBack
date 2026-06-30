-- =====================================================================
-- Tenant (frontera del tenant; NO lleva tenant_id propio)
-- =====================================================================
CREATE TABLE tenants (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    slug              VARCHAR(80)  NOT NULL,
    name              VARCHAR(150) NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    logo_url          VARCHAR(500) NULL,
    color_primario    VARCHAR(20)  NULL,
    fuente            VARCHAR(80)  NULL,
    mostrar_precios   BOOLEAN      NOT NULL DEFAULT TRUE,
    requiere_telefono BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP(6) NOT NULL,
    updated_at        TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_tenants_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Hosts que resuelven a un tenant (landing pública por host)
CREATE TABLE tenant_dominios (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT       NOT NULL,
    host      VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_tenant_dominios_host (host),
    INDEX        idx_tenant_dominios_tenant_id (tenant_id),
    CONSTRAINT fk_tenant_dominios_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- Complejo (local de pádel; la config de agenda vive acá)
-- =====================================================================
CREATE TABLE complejos (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id             BIGINT       NOT NULL,
    nombre                VARCHAR(150) NOT NULL,
    direccion             VARCHAR(255) NULL,
    telefono              VARCHAR(40)  NULL,
    whatsapp              VARCHAR(40)  NULL,
    mapa_url              VARCHAR(500) NULL,
    instagram             VARCHAR(100) NULL,
    paso_minutos          INT          NOT NULL,
    duraciones_permitidas VARCHAR(60)  NOT NULL,
    duracion_default      INT          NOT NULL,
    estado                VARCHAR(20)  NOT NULL,
    active                BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted_at            TIMESTAMP(6) NULL,
    created_at            TIMESTAMP(6) NOT NULL,
    updated_at            TIMESTAMP(6) NOT NULL,
    created_by            VARCHAR(120) NOT NULL,
    updated_by            VARCHAR(120) NOT NULL,
    version               BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_complejos_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- Cancha (recurso reservable; FK directa a un complejo)
-- =====================================================================
CREATE TABLE canchas (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    tenant_id    BIGINT        NOT NULL,
    complejo_id  BIGINT        NOT NULL,
    nombre       VARCHAR(150)  NOT NULL,
    orden        INT           NOT NULL,
    techada      BOOLEAN       NOT NULL,
    tipo_pared   VARCHAR(20)   NOT NULL,
    precio_hora  DECIMAL(10,2) NULL,
    color        VARCHAR(20)   NULL,
    estado       VARCHAR(20)   NOT NULL,
    active       BOOLEAN       NOT NULL DEFAULT TRUE,
    deleted_at   TIMESTAMP(6)  NULL,
    created_at   TIMESTAMP(6)  NOT NULL,
    updated_at   TIMESTAMP(6)  NOT NULL,
    created_by   VARCHAR(120)  NOT NULL,
    updated_by   VARCHAR(120)  NOT NULL,
    version      BIGINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_canchas_tenant_id (tenant_id),
    INDEX idx_canchas_complejo (tenant_id, complejo_id),
    CONSTRAINT fk_canchas_complejo FOREIGN KEY (complejo_id) REFERENCES complejos (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- HorarioComplejo (apertura compartida por todas las canchas; break = 2 filas)
-- =====================================================================
CREATE TABLE horarios_complejo (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id   BIGINT       NOT NULL,
    complejo_id BIGINT       NOT NULL,
    dia_semana  INT          NOT NULL,
    hora_inicio TIME         NOT NULL,
    hora_fin    TIME         NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted_at  TIMESTAMP(6) NULL,
    created_at  TIMESTAMP(6) NOT NULL,
    updated_at  TIMESTAMP(6) NOT NULL,
    created_by  VARCHAR(120) NOT NULL,
    updated_by  VARCHAR(120) NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_horarios_complejo_tenant_id (tenant_id),
    INDEX idx_horarios_complejo_dia (tenant_id, complejo_id, dia_semana),
    CONSTRAINT fk_horarios_complejo FOREIGN KEY (complejo_id) REFERENCES complejos (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- Bloqueo (excepción puntual; cancha_id NULL = todo el complejo)
-- =====================================================================
CREATE TABLE bloqueos (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id        BIGINT       NOT NULL,
    complejo_id      BIGINT       NOT NULL,
    cancha_id        BIGINT       NULL,
    fecha_hora_desde DATETIME(6)  NOT NULL,
    fecha_hora_hasta DATETIME(6)  NOT NULL,
    motivo           VARCHAR(255) NULL,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted_at       TIMESTAMP(6) NULL,
    created_at       TIMESTAMP(6) NOT NULL,
    updated_at       TIMESTAMP(6) NOT NULL,
    created_by       VARCHAR(120) NOT NULL,
    updated_by       VARCHAR(120) NOT NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_bloqueos_tenant_id (tenant_id),
    INDEX idx_bloqueos_complejo_rango (tenant_id, complejo_id, fecha_hora_desde),
    CONSTRAINT fk_bloqueos_complejo FOREIGN KEY (complejo_id) REFERENCES complejos (id),
    CONSTRAINT fk_bloqueos_cancha   FOREIGN KEY (cancha_id)   REFERENCES canchas (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- Reserva (FUENTE DE VERDAD de los turnos; duración variable propia)
-- =====================================================================
CREATE TABLE reservas (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id        BIGINT       NOT NULL,
    complejo_id      BIGINT       NOT NULL,
    cancha_id        BIGINT       NOT NULL,
    cliente_nombre   VARCHAR(150) NOT NULL,
    cliente_whatsapp VARCHAR(40)  NULL,
    inicio           DATETIME(6)  NOT NULL,
    fin              DATETIME(6)  NOT NULL,
    duracion_minutos INT          NOT NULL,
    estado           VARCHAR(20)  NOT NULL,
    ip               VARCHAR(45)  NULL,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted_at       TIMESTAMP(6) NULL,
    created_at       TIMESTAMP(6) NOT NULL,
    updated_at       TIMESTAMP(6) NOT NULL,
    created_by       VARCHAR(120) NOT NULL,
    updated_by       VARCHAR(120) NOT NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_reservas_tenant_id (tenant_id),
    INDEX idx_reservas_cancha_inicio (tenant_id, cancha_id, inicio),
    INDEX idx_reservas_complejo_inicio (tenant_id, complejo_id, inicio),
    CONSTRAINT fk_reservas_complejo FOREIGN KEY (complejo_id) REFERENCES complejos (id),
    CONSTRAINT fk_reservas_cancha   FOREIGN KEY (cancha_id)   REFERENCES canchas (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- Usuario (acceso al panel)
-- =====================================================================
CREATE TABLE usuarios (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id     BIGINT       NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    rol           VARCHAR(20)  NOT NULL,
    estado        VARCHAR(20)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted_at    TIMESTAMP(6) NULL,
    created_at    TIMESTAMP(6) NOT NULL,
    updated_at    TIMESTAMP(6) NOT NULL,
    created_by    VARCHAR(120) NOT NULL,
    updated_by    VARCHAR(120) NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_usuarios_tenant_email (tenant_id, email),
    INDEX        idx_usuarios_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
