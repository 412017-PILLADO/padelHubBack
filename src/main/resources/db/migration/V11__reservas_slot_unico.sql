-- =====================================================================
-- Respaldo en BD contra la doble reserva (C1c).
--
-- El lock pesimista + re-chequeo en transacción nueva (snapshot fresco) es la primera línea de
-- defensa. Esta es la red: una columna generada `slot_activo` que vale `cancha_id|inicio` SOLO
-- cuando la reserva ocupa de verdad (active + estado en PENDIENTE/CONFIRMADO), y NULL en cualquier
-- otro caso (CANCELADO o soft-delete). MySQL permite múltiples NULL en un UNIQUE, así que las
-- canceladas/borradas no estorban.
--
-- El UNIQUE (tenant_id, slot_activo) frena el duplicado EXACTO de inicio en la misma cancha —el caso
-- real, ya que la grilla ancla los inicios—. Los solapes parciales (distinto inicio) los sigue
-- cubriendo el `existeOcupacionVigenteEnCancha` post-lock. Una PENDIENTE vencida no bloquea el
-- re-booking: `crearSiLibre` la pasa a CANCELADO antes del insert y su `slot_activo` vuelve a NULL.
--
-- STORED (no VIRTUAL) para indexarla sin recomputar por fila en cada lectura. IF/CONCAT son
-- deterministas → válidas en una columna generada de MySQL 8.
-- =====================================================================

ALTER TABLE reservas
    ADD COLUMN slot_activo VARCHAR(60)
        GENERATED ALWAYS AS (
            IF(active = TRUE AND estado IN ('PENDIENTE', 'CONFIRMADO'),
               CONCAT(cancha_id, '|', inicio),
               NULL)
        ) STORED;

ALTER TABLE reservas
    ADD CONSTRAINT uq_reservas_slot_activo UNIQUE (tenant_id, slot_activo);
