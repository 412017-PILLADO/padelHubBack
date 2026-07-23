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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
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

    // REQUIRES_NEW (no REQUIRED): CrearReservaUseCase.ejecutar es @Transactional y ya hizo lecturas
    // (disponibilidad, anti-abuso) que, bajo InnoDB REPEATABLE READ, FIJARON el snapshot de la tx.
    // Si nos uniéramos a esa tx, el re-chequeo post-lock leería ese snapshot viejo y NO vería reservas
    // commiteadas por otra tx mientras esperábamos el PESSIMISTIC_WRITE de la cancha → doble reserva.
    // En una transacción nueva el snapshot se abre recién acá, así que las lecturas post-lock son
    // frescas. El TenantContext es ThreadLocal → sobrevive; NuevaReserva es un record puro, no depende
    // de estado JPA de la tx externa.
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReservaCreada crearSiLibre(NuevaReserva nueva) {
        Long tenantId = tenantProvider.requireTenantId();
        LocalDateTime ahora = LocalDateTime.now(clock);

        // Lock pesimista sobre la cancha: serializa creaciones concurrentes para la misma cancha.
        CanchaJpaEntity cancha = canchaRepo.lockByTenantIdAndId(tenantId, nueva.canchaId())
                .orElseThrow(() -> new SlotNoDisponibleException("Cancha no disponible"));

        // Cancela las PENDIENTE ya vencidas que solapan el rango: la disponibilidad ya las ignora
        // (expiración perezosa), pero siguen ocupando el UNIQUE index de respaldo (slot_activo) hasta
        // que el job las barra. Pasarlas a CANCELADO libera el slot para re-bookearlo ahora mismo.
        reservaRepo.cancelarPendientesVencidasSolapadas(
                tenantId, nueva.canchaId(), ahora, nueva.inicio(), nueva.fin());

        boolean ocupado = reservaRepo.existeOcupacionVigenteEnCancha(
                tenantId, nueva.canchaId(), ahora, nueva.inicio(), nueva.fin());
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
        ReservaJpaEntity saved;
        try {
            saved = reservaRepo.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            // Respaldo en BD: el UNIQUE (tenant_id, slot_activo) frenó un duplicado exacto de inicio
            // que ganó la carrera entre el re-chequeo y el flush. Se traduce al mismo 409 de negocio.
            throw new SlotNoDisponibleException("El horario fue tomado, probá otro");
        }

        return new ReservaCreada(saved.getId(), cancha.getId(), cancha.getNombre(),
                saved.getInicio(), saved.getFin(), saved.getDuracionMinutos(), saved.getEstado().name());
    }
}
