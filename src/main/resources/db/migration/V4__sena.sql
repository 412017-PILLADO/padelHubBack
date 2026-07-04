-- =====================================================================
-- Señas manuales (sin pasarela de pago).
--   * complejos.requiere_sena: si TRUE, las reservas nuevas nacen PENDIENTE (a la espera de que el
--     dueño valide la seña "del otro lado del mostrador"); si FALSE nacen CONFIRMADO (como hasta ahora).
--   * complejos.sena_monto: monto informativo de la seña que se le muestra al cliente (no se cobra acá).
--   * reservas.expira_en: momento en que una reserva PENDIENTE deja de retener la cancha. La
--     disponibilidad cuenta como ocupada toda reserva CONFIRMADA o PENDIENTE cuyo expira_en > ahora,
--     así que el slot se libera solo al vencer (expiración perezosa); un job la pasa a CANCELADO luego.
-- Defaults pensados para no romper datos: el módulo arranca apagado (requiere_sena = FALSE) y las
-- reservas existentes (CONFIRMADO) tienen expira_en NULL => siguen ocupando siempre.
-- =====================================================================
ALTER TABLE complejos
    ADD COLUMN requiere_sena BOOLEAN       NOT NULL DEFAULT FALSE AFTER precio_hora_general,
    ADD COLUMN sena_monto    DECIMAL(10,2) NULL                   AFTER requiere_sena;

ALTER TABLE reservas
    ADD COLUMN expira_en DATETIME(6) NULL AFTER estado;

-- Índice para el barrido de pendientes vencidas (job) y el listado de pendientes del panel.
CREATE INDEX idx_reservas_estado_expira ON reservas (estado, expira_en);
