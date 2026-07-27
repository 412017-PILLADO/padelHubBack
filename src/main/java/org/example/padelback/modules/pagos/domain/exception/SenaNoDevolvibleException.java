package org.example.padelback.modules.pagos.domain.exception;

/** La seña no tiene pago aprobado para devolver (o ya fue devuelta). */
public class SenaNoDevolvibleException extends RuntimeException {
    public SenaNoDevolvibleException(String message) {
        super(message);
    }
}
