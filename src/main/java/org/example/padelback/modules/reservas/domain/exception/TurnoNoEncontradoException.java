package org.example.padelback.modules.reservas.domain.exception;

public class TurnoNoEncontradoException extends RuntimeException {
    public TurnoNoEncontradoException(Long id) {
        super("Turno " + id + " no encontrado.");
    }
}
