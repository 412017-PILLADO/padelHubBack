package org.example.padelback.modules.tenant.infrastructure;

import java.util.Optional;

import org.example.padelback.modules.tenant.infrastructure.persistence.entity.TenantDominioJpaEntity;
import org.example.padelback.modules.tenant.infrastructure.persistence.repository.TenantDominioJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class HostTenantResolver {

    private final TenantDominioJpaRepository repo;

    @Transactional(readOnly = true)
    public Optional<Long> resolve(String host) {
        if (host == null || host.isBlank()) {
            return Optional.empty();
        }
        return repo.findByHost(host.trim().toLowerCase()).map(TenantDominioJpaEntity::getTenantId);
    }
}
