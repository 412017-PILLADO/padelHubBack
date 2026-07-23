-- Rebranding de plataforma: la identidad de Padel-HUB pasa del cobalto (#2747ff) al teal/aqua del
-- logo nuevo (#0a8a99 cuerpo, #38c8d8 grip). El tenant demo (la vidriera) se actualiza a la paleta
-- nueva; los clubes reales conservan el color que eligieron.
UPDATE tenants
   SET color_primario = '#0a8a99',
       color_secundario = '#38c8d8'
 WHERE slug = 'demo'
   AND color_primario = '#2747ff';
