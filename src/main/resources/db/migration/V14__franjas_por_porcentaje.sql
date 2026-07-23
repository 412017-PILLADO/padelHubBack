-- Las franjas de precio dejan de ser un precio/hora absoluto (aplastaba la diferencia entre
-- canchas en modo POR_CANCHA) y pasan a ser un AJUSTE PORCENTUAL sobre el precio original:
-- negativo = descuento (-20 → paga 80%), positivo = recargo (+15 → paga 115%). Cada cancha
-- mantiene su precio relativo. No hay conversión posible de $ absolutos a %, así que las franjas
-- existentes se descartan (feature recién salida, sin clientes en producción).
DELETE FROM precio_franjas;

ALTER TABLE precio_franjas
    DROP COLUMN precio_hora,
    ADD COLUMN ajuste_porcentaje INT NOT NULL AFTER hora_hasta;
