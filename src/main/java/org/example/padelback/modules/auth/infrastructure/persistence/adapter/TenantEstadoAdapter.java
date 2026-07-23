package org.example.padelback.modules.auth.infrastructure.persistence.adapter;

import org.example.padelback.modules.auth.domain.port.TenantEstadoPort;
import org.example.padelback.modules.tenant.domain.model.TenantStatus;
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
    public boolean estaActivo(Long tenantId) {
        return tenantId != null
                && tenantRepo.findById(tenantId).map(t -> t.getStatus() == TenantStatus.ACTIVE).orElse(false);
    }
}
