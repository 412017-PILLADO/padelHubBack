package org.example.padelback.modules.reservas.application;

import java.util.List;

import org.example.padelback.domain.port.TenantProvider;
import org.example.padelback.modules.reservas.domain.exception.TurnoNoEncontradoException;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.ArrepentimientoJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.ArrepentimientoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestión desde el panel de las solicitudes de arrepentimiento (Res. 424/2020). */
@Service
@RequiredArgsConstructor
public class GestionArrepentimientosUseCase {

    private final ArrepentimientoJpaRepository repo;
    private final TenantProvider tenantProvider;

    @Transactional(readOnly = true)
    public List<ArrepentimientoJpaEntity> listar() {
        return repo.findByTenantIdAndActiveTrueOrderByGestionadoAscCreatedAtDesc(
                tenantProvider.requireTenantId());
    }

    @Transactional
    public void marcarGestionado(Long id) {
        ArrepentimientoJpaEntity a = repo.findByTenantIdAndIdAndActiveTrue(
                        tenantProvider.requireTenantId(), id)
                .orElseThrow(() -> new TurnoNoEncontradoException(id));
        a.setGestionado(true);
        repo.save(a);
    }
}
