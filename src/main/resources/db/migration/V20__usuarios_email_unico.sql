-- El login resuelve el club por email: el email tiene que identificar a una persona en TODA la
-- plataforma, no dentro de un club. El unique compuesto (tenant_id, email) se CONSERVA: pasa a ser
-- redundante para la unicidad pero sigue sirviendo como índice, y borrarlo agrega riesgo sin ganar
-- nada.
--
-- El UPDATE va primero porque hasta ahora convivían dos formas de escribir el mismo email:
-- TenantProvisioningService normalizaba a minúsculas y OwnerSeeder no.
--
-- Ojo: este UPDATE puede chocar contra el único COMPUESTO (tenant_id, email) que ya existe, si un
-- mismo club tiene 'A@x.com' y 'a@x.com' sembrados por separado — fallaría acá, en el UPDATE, no
-- en el CREATE UNIQUE INDEX de abajo. El chequeo previo a aplicar esta migración lo detecta.
UPDATE usuarios SET email = LOWER(TRIM(email));

CREATE UNIQUE INDEX idx_usuarios_email ON usuarios (email);
