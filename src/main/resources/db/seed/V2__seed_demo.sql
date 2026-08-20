-- =====================================================================
-- Seed de un complejo demo. Pensado para ser duplicable (alta de tenant = duplicar este bloque).
-- El usuario OWNER lo crea el OwnerSeeder (resuelve el tenant por el host 'localhost').
-- IDs explícitos para que el seed sea determinístico y las FKs sean simples.
-- =====================================================================

-- Tenant
INSERT INTO tenants (id, slug, name, status, color_primario, fuente, mostrar_precios, requiere_telefono, created_at, updated_at)
VALUES (1, 'demo', 'Padel Hub Demo', 'ACTIVE', '#2747ff', 'Hanken Grotesk', TRUE, TRUE, NOW(6), NOW(6));

-- Dominios que resuelven al tenant: 'localhost' (panel/dev) y 'demo.localhost' (landing por subdominio)
INSERT INTO tenant_dominios (tenant_id, host) VALUES (1, 'localhost');
INSERT INTO tenant_dominios (tenant_id, host) VALUES (1, 'demo.localhost');

-- Complejo: paso de 30 min, duraciones 60/90/120 (default 90)
INSERT INTO complejos (id, tenant_id, nombre, direccion, telefono, whatsapp, mapa_url, instagram,
                       paso_minutos, duraciones_permitidas, duracion_default, estado,
                       active, created_at, updated_at, created_by, updated_by, version)
VALUES (1, 1, 'Padel Hub Demo', 'Av. Siempre Viva 742, Córdoba', '3510000000', '5493510000000',
        NULL, 'padelhubdemo', 30, '60,90,120', 90, 'ACTIVO',
        TRUE, NOW(6), NOW(6), 'seed', 'seed', 0);

-- Canchas
INSERT INTO canchas (id, tenant_id, complejo_id, nombre, orden, techada, tipo_pared, precio_hora, color, estado,
                     active, created_at, updated_at, created_by, updated_by, version) VALUES
 (1, 1, 1, 'Cancha 1', 1, TRUE,  'CRISTAL', 8000.00, '#2747ff', 'ACTIVO', TRUE, NOW(6), NOW(6), 'seed', 'seed', 0),
 (2, 1, 1, 'Cancha 2', 2, FALSE, 'CRISTAL', 7000.00, '#1faa59', 'ACTIVO', TRUE, NOW(6), NOW(6), 'seed', 'seed', 0),
 (3, 1, 1, 'Cancha 3', 3, FALSE, 'MURO',    6000.00, '#e0392b', 'ACTIVO', TRUE, NOW(6), NOW(6), 'seed', 'seed', 0);

-- Horarios del complejo: todos los días 08:00 - 23:00 (sin cierre al mediodía)
INSERT INTO horarios_complejo (tenant_id, complejo_id, dia_semana, hora_inicio, hora_fin,
                               active, created_at, updated_at, created_by, updated_by, version) VALUES
 (1, 1, 0, '08:00:00', '23:00:00', TRUE, NOW(6), NOW(6), 'seed', 'seed', 0),
 (1, 1, 1, '08:00:00', '23:00:00', TRUE, NOW(6), NOW(6), 'seed', 'seed', 0),
 (1, 1, 2, '08:00:00', '23:00:00', TRUE, NOW(6), NOW(6), 'seed', 'seed', 0),
 (1, 1, 3, '08:00:00', '23:00:00', TRUE, NOW(6), NOW(6), 'seed', 'seed', 0),
 (1, 1, 4, '08:00:00', '23:00:00', TRUE, NOW(6), NOW(6), 'seed', 'seed', 0),
 (1, 1, 5, '08:00:00', '23:00:00', TRUE, NOW(6), NOW(6), 'seed', 'seed', 0),
 (1, 1, 6, '08:00:00', '23:00:00', TRUE, NOW(6), NOW(6), 'seed', 'seed', 0);
