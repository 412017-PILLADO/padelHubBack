-- Política de cancelación/devolución del complejo (texto libre del dueño, visible en la landing).
ALTER TABLE complejos ADD COLUMN politica_cancelacion TEXT NULL;
