package org.example.padelback.modules.pagos.domain.exception;

/** La reserva no admite link de pago (no está PENDIENTE, ya venció, o el complejo no pide seña). */
public class LinkSenaNoDisponibleException extends RuntimeException {
    public LinkSenaNoDisponibleException(String message) {
        super(message);
    }
}
