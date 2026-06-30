package org.example.padelback.modules.reservas.domain.exception;

/** Datos de cancha inválidos al crear/editar desde el panel (tipo de pared, estado, precio, etc.). */
public class CanchaInvalidaException extends RuntimeException {
    public CanchaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
