package org.example.padelback.domain.port;

import java.util.Optional;

import org.example.padelback.domain.exception.TenantNotResolvedException;

public interface TenantProvider {

    Optional<Long> currentTenantId();

    default Long requireTenantId() {
        return currentTenantId().orElseThrow(TenantNotResolvedException::new);
    }
}
