package org.example.padelback.modules.reservas.domain.exception;

/**
 * La reserva no se puede validar como seña: o no está PENDIENTE, o su ventana de pago ya venció (y el
 * slot pudo haberse liberado). Se traduce a 409 para que el panel refresque la lista de pendientes.
 */
public class SenaNoValidableException extends RuntimeException {
    public SenaNoValidableException(String mensaje) {
        super(mensaje);
    }
}
