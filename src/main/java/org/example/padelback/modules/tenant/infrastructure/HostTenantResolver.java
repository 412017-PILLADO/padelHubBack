package org.example.padelback.modules.tenant.infrastructure;

import java.util.Optional;

import org.example.padelback.modules.tenant.domain.model.TenantStatus;
import org.example.padelback.modules.tenant.infrastructure.persistence.entity.TenantDominioJpaEntity;
import org.example.padelback.modules.tenant.infrastructure.persistence.repository.TenantDominioJpaRepository;
import org.example.padelback.modules.tenant.infrastructure.persistence.repository.TenantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Resuelve el tenant por host ({@code tenant_dominios}); solo devuelve tenants ACTIVE. */
@Component
@RequiredArgsConstructor
public class HostTenantResolver {

    private final TenantDominioJpaRepository repo;
    private final TenantJpaRepository tenantRepo;

    @Transactional(readOnly = true)
    public Optional<Long> resolve(String host) {
        if (host == null || host.isBlank()) {
            return Optional.empty();
        }
        return repo.findByHost(host.trim().toLowerCase())
                .map(TenantDominioJpaEntity::getTenantId)
                .filter(tenantId -> tenantRepo.findByIdAndStatus(tenantId, TenantStatus.ACTIVE).isPresent());
    }
}
