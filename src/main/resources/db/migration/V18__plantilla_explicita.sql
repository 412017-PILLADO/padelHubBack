-- Congela la plantilla de los clubes que YA EXISTEN antes de que la default del producto pase de A
-- a C (ver docs/superpowers/specs/2026-08-16-plantilla-c-basica-design.md, seccion 5.2).
--
-- Sin esto, un club que nunca eligio plantilla esta viendo A por el fallback del front, y el dia que
-- la default cambie le cambia la pagina publica sin que haya tocado nada. Con esto, el cambio de
-- default aplica SOLO a clubes nuevos.
--
-- Es idempotente a proposito: si se corre dos veces no pisa a nadie que ya haya elegido.
UPDATE tenants
   SET plantilla = 'A'
 WHERE plantilla IS NULL
    OR TRIM(plantilla) = '';
