-- =====================================================================
-- Turno principal + modo de precio del complejo.
--   * permitir_otras_duraciones: si FALSE, el cliente solo reserva el turno principal
--     (duracion_default); si TRUE se ofrecen todas las duraciones permitidas, ancladas a la
--     grilla del turno principal.
--   * precio_modo: GENERAL = un precio por hora para todo el complejo; POR_CANCHA = el de cada cancha.
--   * precio_hora_general: precio por hora usado cuando precio_modo = GENERAL.
-- Defaults pensados para no romper datos existentes: se siguen ofreciendo las duraciones cargadas
-- y el precio sigue siendo por cancha (como hasta ahora).
-- =====================================================================
ALTER TABLE complejos
    ADD COLUMN permitir_otras_duraciones BOOLEAN       NOT NULL DEFAULT TRUE  AFTER duracion_default,
    ADD COLUMN precio_modo               VARCHAR(20)   NOT NULL DEFAULT 'POR_CANCHA' AFTER permitir_otras_duraciones,
    ADD COLUMN precio_hora_general       DECIMAL(10,2) NULL                    AFTER precio_modo;
