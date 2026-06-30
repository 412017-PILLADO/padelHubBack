package org.example.padelback.modules.reservas.domain.exception;

public class CanchaNoEncontradaException extends RuntimeException {
    public CanchaNoEncontradaException(Long id) {
        super("Cancha " + id + " no encontrada.");
    }
}
