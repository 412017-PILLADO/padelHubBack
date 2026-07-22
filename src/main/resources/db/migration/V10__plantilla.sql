-- =====================================================================
-- Plantilla de landing del tenant: A (poster, actual/default), B (hero centrado), C (compacta
-- tipo app). Cambia SOLO el layout de la landing pública; el flujo de reserva es el mismo en las
-- tres. NOT NULL con default 'A' → los tenants existentes quedan en la plantilla actual sin
-- necesidad de migrar datos.
-- =====================================================================
ALTER TABLE tenants ADD COLUMN plantilla VARCHAR(1) NOT NULL DEFAULT 'A' AFTER color_secundario;
