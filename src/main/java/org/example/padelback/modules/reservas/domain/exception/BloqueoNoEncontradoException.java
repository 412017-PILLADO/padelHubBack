package org.example.padelback.modules.reservas.domain.exception;

public class BloqueoNoEncontradoException extends RuntimeException {
    public BloqueoNoEncontradoException(Long id) {
        super("Bloqueo " + id + " no encontrado.");
    }
}
