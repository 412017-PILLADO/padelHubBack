package org.example.padelback.modules.reservas.infrastructure.persistence.adapter;

import org.example.padelback.domain.port.TenantProvider;
import org.example.padelback.modules.reservas.domain.exception.TurnoNoEncontradoException;
import org.example.padelback.modules.reservas.domain.model.ReservaEstado;
import org.example.padelback.modules.reservas.domain.port.TurnoCommandPort;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.ReservaJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.ReservaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class TurnoCommandAdapter implements TurnoCommandPort {

    private final TenantProvider tenantProvider;
    private final ReservaJpaRepository reservaRepo;

    @Override
    @Transactional
    public void cancelar(Long turnoId) {
        Long tenantId = tenantProvider.requireTenantId();
        ReservaJpaEntity reserva = reservaRepo.findByTenantIdAndIdAndActiveTrue(tenantId, turnoId)
                .orElseThrow(() -> new TurnoNoEncontradoException(turnoId));
        reserva.setEstado(ReservaEstado.CANCELADO);
        reservaRepo.save(reserva);
    }
}
