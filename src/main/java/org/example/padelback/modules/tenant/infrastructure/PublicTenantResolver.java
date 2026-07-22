package org.example.padelback.modules.tenant.infrastructure;

import java.util.Optional;

import org.example.padelback.modules.tenant.domain.model.TenantStatus;
import org.example.padelback.modules.tenant.infrastructure.persistence.entity.TenantJpaEntity;
import org.example.padelback.modules.tenant.infrastructure.persistence.repository.TenantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolución del tenant para la superficie pública (front y back desplegados por separado).
 *
 * <p>Prioriza el <b>slug</b> que manda el front en el header {@code X-Tenant} (derivado de su
 * subdominio, ej. {@code filonavaja.tuapp.com → "filonavaja"}); si no viene, cae a la resolución
 * por <b>host</b> ({@code tenant_dominios}) para no romper accesos same-origin ni el dev actual.
 *
 * <p>Solo resuelve tenants {@link TenantStatus#ACTIVE}: uno INACTIVE (suspendido/dado de baja
 * lógicamente) no debe servir su landing pública ni dejar loguear a sus owners.
 */
@Component
@RequiredArgsConstructor
public class PublicTenantResolver {

    private final TenantJpaRepository tenantRepo;
    private final HostTenantResolver hostResolver;

    @Transactional(readOnly = true)
    public Optional<Long> resolve(String tenantSlugHeader, String host) {
        if (tenantSlugHeader != null && !tenantSlugHeader.isBlank()) {
            return tenantRepo.findBySlugAndStatus(tenantSlugHeader.trim().toLowerCase(), TenantStatus.ACTIVE)
                    .map(TenantJpaEntity::getId);
        }
        return hostResolver.resolve(host);
    }
}
