package org.example.padelback.infrastructure.persistence.entity;

import java.util.Objects;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import org.example.padelback.domain.exception.TenantNotResolvedException;
import org.example.padelback.infrastructure.tenancy.TenantContext;

public class TenantEntityListener {

    @PrePersist
    public void beforePersist(BaseJpaEntity entity) {
        TenantContext.getTenantId().ifPresentOrElse(
                currentTenantId -> {
                    if (entity.getTenantId() == null) {
                        entity.setTenantId(currentTenantId);
                    } else {
                        assertSameTenant(entity, currentTenantId);
                    }
                },
                () -> {
                    if (entity.getTenantId() == null) {
                        throw new TenantNotResolvedException();
                    }
                }
        );
    }

    @PreUpdate
    public void beforeUpdate(BaseJpaEntity entity) {
        TenantContext.getTenantId().ifPresent(currentTenantId -> assertSameTenant(entity, currentTenantId));
    }

    private void assertSameTenant(BaseJpaEntity entity, Long currentTenantId) {
        if (!Objects.equals(entity.getTenantId(), currentTenantId)) {
            throw new TenantNotResolvedException("Entity tenant does not match the current request tenant.");
        }
    }
}
