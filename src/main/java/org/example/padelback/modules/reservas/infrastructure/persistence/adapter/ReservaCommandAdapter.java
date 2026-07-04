package org.example.padelback.modules.reservas.infrastructure.persistence.adapter;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.example.padelback.domain.port.TenantProvider;
import org.example.padelback.modules.reservas.domain.exception.SlotNoDisponibleException;
import org.example.padelback.modules.reservas.domain.model.reserva.NuevaReserva;
import org.example.padelback.modules.reservas.domain.model.reserva.ReservaCreada;
import org.example.padelback.modules.reservas.domain.port.ReservaCommandPort;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.CanchaJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.ReservaJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.CanchaJpaRepository;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.ReservaJpaRepository;
import org.example.padelback.modules.tenant.infrastructure.persistence.repository.TenantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class ReservaCommandAdapter implements ReservaCommandPort {

    private final TenantProvider tenantProvider;
    private final TenantJpaRepository tenantRepo;
    private final ReservaJpaRepository reservaRepo;
    private final CanchaJpaRepository canchaRepo;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public boolean tenantRequiereTelefono() {
        Long tenantId = tenantProvider.requireTenantId();
        return tenantRepo.findById(tenantId).map(t -> t.isRequiereTelefono()).orElse(true);
    }

    @Override
    @Transactional(readOnly = true)
    public int reservasQueOcupanEseDia(Long canchaId, LocalDate fecha) {
        Long tenantId = tenantProvider.requireTenantId();
        LocalDateTime desde = fecha.atStartOfDay();
        LocalDateTime hasta = fecha.plusDays(1).atStartOfDay();
        return reservaRepo.contarOcupacionVigenteEseDia(
                tenantId, canchaId, LocalDateTime.now(clock), desde, hasta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> telefonosDeReservasActivasFuturas(LocalDateTime ahora) {
        Long tenantId = tenantProvider.requireTenantId();
        return reservaRepo.clienteWhatsappDeActivasFuturas(tenantId, ahora);
    }

    @Override
    @Transactional(readOnly = true)
    public int reservasDesdeIpDesde(String ip, Instant desde) {
        Long tenantId = tenantProvider.requireTenantId();
        return reservaRepo.countByTenantIdAndIpAndCreatedAtGreaterThanEqual(tenantId, ip, desde);
    }

    @Override
    @Transactional
    public ReservaCreada crearSiLibre(NuevaReserva nueva) {
        Long tenantId = tenantProvider.requireTenantId();

        // Lock pesimista sobre la cancha: serializa creaciones concurrentes para la misma cancha.
        CanchaJpaEntity cancha = canchaRepo.lockByTenantIdAndId(tenantId, nueva.canchaId())
                .orElseThrow(() -> new SlotNoDisponibleException("Cancha no disponible"));

        boolean ocupado = reservaRepo.existeOcupacionVigenteEnCancha(
                tenantId, nueva.canchaId(), LocalDateTime.now(clock), nueva.inicio(), nueva.fin());
        if (ocupado) {
            throw new SlotNoDisponibleException("El horario fue tomado, probá otro");
        }

        ReservaJpaEntity entity = ReservaJpaEntity.builder()
                .complejoId(nueva.complejoId())
                .canchaId(nueva.canchaId())
                .clienteNombre(nueva.clienteNombre())
                .clienteWhatsapp(nueva.clienteWhatsapp())
                .ip(nueva.ip())
                .inicio(nueva.inicio())
                .fin(nueva.fin())
                .duracionMinutos(nueva.duracionMinutos())
                .estado(nueva.estado())
                .expiraEn(nueva.expiraEn())
                .build();
        // tenant_id lo estampa TenantEntityListener desde el TenantContext.
        ReservaJpaEntity saved = reservaRepo.saveAndFlush(entity);

        return new ReservaCreada(saved.getId(), cancha.getId(), cancha.getNombre(),
                saved.getInicio(), saved.getFin(), saved.getDuracionMinutos(), saved.getEstado().name());
    }
}
