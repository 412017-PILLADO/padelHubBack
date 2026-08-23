package org.example.padelback.modules.auth.infrastructure.persistence.adapter;

import java.util.Optional;

import org.example.padelback.modules.auth.domain.port.TenantEstadoPort;
import org.example.padelback.modules.tenant.domain.model.TenantStatus;
import org.example.padelback.modules.tenant.infrastructure.persistence.entity.TenantJpaEntity;
import org.example.padelback.modules.tenant.infrastructure.persistence.repository.TenantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapter chico de {@link TenantEstadoPort}: lee el status directo de {@code tenants} vía el
 * repositorio del módulo tenant. El dominio de auth ({@link org.example.padelback.modules.auth.application.LoginUseCase})
 * solo conoce el puerto, no esta implementación.
 */
@Component
@RequiredArgsConstructor
public class TenantEstadoAdapter implements TenantEstadoPort {

    private final TenantJpaRepository tenantRepo;

    @Override
    @Transactional(readOnly = true)
    public Optional<String> slugSiActivo(Long tenantId) {
        if (tenantId == null) {
            return Optional.empty();
        }
        return tenantRepo.findById(tenantId)
                .filter(t -> t.getStatus() == TenantStatus.ACTIVE)
                .map(TenantJpaEntity::getSlug);
    }
}
