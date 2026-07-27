package org.example.padelback.modules.pagos.domain.exception;

/** El tenant no tiene su cuenta de MP conectada (o la credencial venció). */
public class MpNoConectadoException extends RuntimeException {
    public MpNoConectadoException() {
        super("El club no tiene Mercado Pago conectado.");
    }
}
