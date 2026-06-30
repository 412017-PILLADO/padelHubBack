package org.example.padelback.domain.exception;

public class TenantNotResolvedException extends RuntimeException {

    public TenantNotResolvedException() {
        super("Tenant could not be resolved for the current request.");
    }

    public TenantNotResolvedException(String message) {
        super(message);
    }
}
