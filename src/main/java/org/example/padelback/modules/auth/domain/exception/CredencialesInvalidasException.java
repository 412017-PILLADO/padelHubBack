package org.example.padelback.modules.auth.domain.exception;

public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Email o contraseña inválidos.");
    }
}
