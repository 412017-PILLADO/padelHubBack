package org.example.padelback.modules.reservas.infrastructure.persistence.repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.example.padelback.modules.reservas.domain.model.ReservaEstado;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.ReservaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaJpaRepository extends JpaRepository<ReservaJpaEntity, Long> {

    // --- Disponibilidad: reservas CONFIRMADAS del complejo que solapan [desde, hasta) ---
    // (inicio < hasta AND fin > desde)
    List<ReservaJpaEntity> findByTenantIdAndComplejoIdAndEstadoAndActiveTrueAndInicioLessThanAndFinGreaterThan(
            Long tenantId, Long complejoId, ReservaEstado estado, LocalDateTime hasta, LocalDateTime desde);

    // --- Anti-doble-reserva: ¿hay una reserva CONFIRMADA de la cancha que solape [desde, hasta)? ---
    boolean existsByTenantIdAndCanchaIdAndEstadoAndActiveTrueAndInicioLessThanAndFinGreaterThan(
            Long tenantId, Long canchaId, ReservaEstado estado, LocalDateTime hasta, LocalDateTime desde);

    // --- Carga del día de una cancha (para "menos cargada" al auto-asignar) ---
    int countByTenantIdAndCanchaIdAndEstadoAndActiveTrueAndInicioGreaterThanEqualAndInicioLessThan(
            Long tenantId, Long canchaId, ReservaEstado estado, LocalDateTime desde, LocalDateTime hasta);

    // --- Panel: turnos CONFIRMADOS del día, ordenados por inicio ---
    List<ReservaJpaEntity> findByTenantIdAndEstadoAndActiveTrueAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
            Long tenantId, ReservaEstado estado, LocalDateTime desde, LocalDateTime hasta);

    Optional<ReservaJpaEntity> findByTenantIdAndIdAndActiveTrue(Long tenantId, Long id);

    // --- Anti-abuso ---
    int countByTenantIdAndIpAndCreatedAtGreaterThanEqual(Long tenantId, String ip, Instant desde);

    @Query("select r.clienteWhatsapp from ReservaJpaEntity r where r.tenantId = :tenantId "
            + "and r.estado = :estado and r.active = true and r.inicio > :ahora")
    List<String> clienteWhatsappDeActivasFuturas(@Param("tenantId") Long tenantId,
            @Param("estado") ReservaEstado estado, @Param("ahora") LocalDateTime ahora);

    // --- Cleanup (hard delete) de reservas pasadas: no hay histórico de turnos (decisión de producto). ---
    @Modifying
    @Query(value = "DELETE FROM reservas WHERE fin < :cutoff", nativeQuery = true)
    int deleteByFinBefore(@Param("cutoff") LocalDateTime cutoff);
}
