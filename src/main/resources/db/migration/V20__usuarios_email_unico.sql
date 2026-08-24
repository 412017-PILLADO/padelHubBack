-- El login resuelve el club por email: el email tiene que identificar a una persona en TODA la
-- plataforma, no dentro de un club. El unique compuesto (tenant_id, email) se CONSERVA: pasa a ser
-- redundante para la unicidad pero sigue sirviendo como índice, y borrarlo agrega riesgo sin ganar
-- nada.
--
-- El UPDATE va primero porque hasta ahora convivían dos formas de escribir el mismo email:
-- TenantProvisioningService normalizaba a minúsculas y OwnerSeeder no.
UPDATE usuarios SET email = LOWER(TRIM(email));

CREATE UNIQUE INDEX idx_usuarios_email ON usuarios (email);
