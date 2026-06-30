package org.example.padelback.infrastructure.tenancy;

import java.util.Optional;
import java.util.function.Supplier;

import org.example.padelback.domain.exception.TenantNotResolvedException;

public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    /**
     * Ejecuta {@code action} con el contexto fijado en {@code tenantId} y restaura el valor
     * previo (o lo limpia) al salir. Para escrituras cross-tenant legítimas (ej. provisioning
     * del seed o alta de un tenant nuevo por el dev).
     */
    public static <T> T runAs(Long tenantId, Supplier<T> action) {
        Optional<Long> previous = getTenantId();
        try {
            setTenantId(tenantId);
            return action.get();
        } finally {
            previous.ifPresentOrElse(TenantContext::setTenantId, TenantContext::clear);
        }
    }

    public static void setTenantId(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new TenantNotResolvedException("Tenant id must be a positive number.");
        }
        CURRENT_TENANT.set(tenantId);
    }

    public static Optional<Long> getTenantId() {
        return Optional.ofNullable(CURRENT_TENANT.get());
    }

    public static Long requireTenantId() {
        return getTenantId().orElseThrow(TenantNotResolvedException::new);
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
