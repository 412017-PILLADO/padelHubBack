-- =====================================================================
-- Alias de transferencia para la seña.
--   * complejos.sena_alias: alias / CBU / CVU al que el cliente transfiere la seña. Se le muestra en
--     la pantalla de éxito con un botón para copiarlo. Obligatorio (junto al monto) cuando el módulo
--     de señas está activo; informativo, no se valida contra ningún banco.
-- Nullable para no romper datos existentes (complejos sin seña o creados antes de esta columna).
-- =====================================================================
ALTER TABLE complejos
    ADD COLUMN sena_alias VARCHAR(100) NULL AFTER sena_monto;

-- El complejo demo trae un alias de ejemplo para que la feature se vea al activar la seña.
UPDATE complejos SET sena_alias = 'padel.hub.demo' WHERE id = 1;
