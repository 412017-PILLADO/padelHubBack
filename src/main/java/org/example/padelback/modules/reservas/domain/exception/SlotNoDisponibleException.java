package org.example.padelback.modules.reservas.domain.exception;

public class SlotNoDisponibleException extends RuntimeException {
    public SlotNoDisponibleException(String message) {
        super(message);
    }
}
