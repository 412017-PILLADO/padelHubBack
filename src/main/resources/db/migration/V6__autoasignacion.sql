-- =====================================================================
-- Autoasignación de canchas.
--   * complejos.autoasignacion: si TRUE, el sistema asigna una cancha disponible automáticamente
--     (la menos cargada, como "cualquiera") y la landing NO le muestra al cliente el paso de elegir
--     cancha. Útil cuando las canchas son equivalentes (mismo precio/tipo) y elegir es al pedo.
-- Default FALSE: el comportamiento por defecto sigue siendo que el cliente elija la cancha.
-- =====================================================================
ALTER TABLE complejos
    ADD COLUMN autoasignacion BOOLEAN NOT NULL DEFAULT FALSE AFTER sena_alias;
