package org.example.padelback.modules.pagos.domain.exception;

/** La plataforma no tiene app de MP configurada (client-id/secret vacíos). */
public class MpNoConfiguradoException extends RuntimeException {
    public MpNoConfiguradoException() {
        super("Mercado Pago no está habilitado en esta plataforma.");
    }
}
