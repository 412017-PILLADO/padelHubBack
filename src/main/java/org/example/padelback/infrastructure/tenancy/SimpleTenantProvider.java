package org.example.padelback.infrastructure.tenancy;

import java.util.Optional;

import org.example.padelback.domain.port.TenantProvider;
import org.springframework.stereotype.Component;

@Component
public class SimpleTenantProvider implements TenantProvider {

    @Override
    public Optional<Long> currentTenantId() {
        return TenantContext.getTenantId();
    }
}
