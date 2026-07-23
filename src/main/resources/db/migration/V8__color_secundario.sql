-- =====================================================================
-- Color secundario del tenant: acento secundario de la marca (ej. el grip de la paleta y otros
-- detalles). Nullable → si no se define, el front cae al color primario. El primario ya vive en
-- `tenants.color_primario`; este lo acompaña.
-- =====================================================================
ALTER TABLE tenants ADD COLUMN color_secundario VARCHAR(20) NULL AFTER color_primario;
