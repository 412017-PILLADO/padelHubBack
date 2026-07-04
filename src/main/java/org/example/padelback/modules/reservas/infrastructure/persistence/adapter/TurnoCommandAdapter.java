package org.example.padelback.modules.reservas.infrastructure.persistence.adapter;

import java.time.Clock;
import java.time.LocalDateTime;

import org.example.padelback.domain.port.TenantProvider;
import org.example.padelback.modules.reservas.domain.exception.SenaNoValidableException;
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
    private final Clock clock;

    @Override
    @Transactional
    public void cancelar(Long turnoId) {
        ReservaJpaEntity reserva = cargar(turnoId);
        reserva.setEstado(ReservaEstado.CANCELADO);
        reserva.setExpiraEn(null);
        reservaRepo.save(reserva);
    }

    @Override
    @Transactional
    public void confirmarSena(Long turnoId) {
        ReservaJpaEntity reserva = cargar(turnoId);
        if (reserva.getEstado() != ReservaEstado.PENDIENTE) {
            throw new SenaNoValidableException("La reserva no está pendiente de seña.");
        }
        // Una PENDIENTE vigente tiene la cancha reservada, así que confirmarla es seguro. Si ya venció,
        // el slot pudo tomarlo otro: no se puede confirmar (el panel debe refrescar la lista).
        if (reserva.getExpiraEn() != null && !reserva.getExpiraEn().isAfter(LocalDateTime.now(clock))) {
            throw new SenaNoValidableException("La seña venció; la reserva ya no se puede confirmar.");
        }
        reserva.setEstado(ReservaEstado.CONFIRMADO);
        reserva.setExpiraEn(null);
        reservaRepo.save(reserva);
    }

    @Override
    @Transactional
    public void rechazarSena(Long turnoId) {
        ReservaJpaEntity reserva = cargar(turnoId);
        if (reserva.getEstado() != ReservaEstado.PENDIENTE) {
            throw new SenaNoValidableException("La reserva no está pendiente de seña.");
        }
        reserva.setEstado(ReservaEstado.CANCELADO);
        reserva.setExpiraEn(null);
        reservaRepo.save(reserva);
    }

    private ReservaJpaEntity cargar(Long turnoId) {
        Long tenantId = tenantProvider.requireTenantId();
        return reservaRepo.findByTenantIdAndIdAndActiveTrue(tenantId, turnoId)
                .orElseThrow(() -> new TurnoNoEncontradoException(turnoId));
    }
}
