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

    // Predicado "ocupa el slot": la reserva no está cancelada y, si es una seña PENDIENTE, todavía
    // está vigente (expira_en > ahora). Las CONFIRMADAS llevan expira_en null => ocupan siempre.
    // Un PENDIENTE vencido deja de ocupar de inmediato (expiración perezosa), sin esperar al job.
    String OCUPA = "r.active = true "
            + "and r.estado <> org.example.padelback.modules.reservas.domain.model.ReservaEstado.CANCELADO "
            + "and (r.expiraEn is null or r.expiraEn > :ahora) ";

    // --- Disponibilidad: reservas del complejo que ocupan y solapan [desde, hasta) (inicio < hasta AND fin > desde) ---
    @Query("select r from ReservaJpaEntity r where r.tenantId = :tenantId and r.complejoId = :complejoId "
            + "and " + OCUPA + "and r.inicio < :hasta and r.fin > :desde")
    List<ReservaJpaEntity> ocupacionesVigentesDelComplejo(@Param("tenantId") Long tenantId,
            @Param("complejoId") Long complejoId, @Param("ahora") LocalDateTime ahora,
            @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    // --- Anti-doble-reserva: ¿hay una reserva vigente de la cancha que solape [desde, hasta)? ---
    @Query("select count(r) > 0 from ReservaJpaEntity r where r.tenantId = :tenantId and r.canchaId = :canchaId "
            + "and " + OCUPA + "and r.inicio < :hasta and r.fin > :desde")
    boolean existeOcupacionVigenteEnCancha(@Param("tenantId") Long tenantId, @Param("canchaId") Long canchaId,
            @Param("ahora") LocalDateTime ahora, @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    // --- Carga del día de una cancha (para "menos cargada" al auto-asignar): cuenta las que ocupan ---
    @Query("select count(r) from ReservaJpaEntity r where r.tenantId = :tenantId and r.canchaId = :canchaId "
            + "and " + OCUPA + "and r.inicio >= :desde and r.inicio < :hasta")
    int contarOcupacionVigenteEseDia(@Param("tenantId") Long tenantId, @Param("canchaId") Long canchaId,
            @Param("ahora") LocalDateTime ahora, @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    // --- Panel: turnos CONFIRMADOS del día, ordenados por inicio ---
    List<ReservaJpaEntity> findByTenantIdAndEstadoAndActiveTrueAndInicioGreaterThanEqualAndInicioLessThanOrderByInicioAsc(
            Long tenantId, ReservaEstado estado, LocalDateTime desde, LocalDateTime hasta);

    // --- Panel: pendientes de seña todavía vigentes (todas las fechas), más urgentes primero ---
    List<ReservaJpaEntity> findByTenantIdAndEstadoAndActiveTrueAndExpiraEnGreaterThanOrderByExpiraEnAsc(
            Long tenantId, ReservaEstado estado, LocalDateTime ahora);

    Optional<ReservaJpaEntity> findByTenantIdAndIdAndActiveTrue(Long tenantId, Long id);

    // --- Anti-abuso ---
    int countByTenantIdAndIpAndCreatedAtGreaterThanEqual(Long tenantId, String ip, Instant desde);

    // Teléfonos de reservas que ocupan (CONFIRMADO + PENDIENTE vigente) con inicio futuro: una seña
    // pendiente ya cuenta para el límite por teléfono, así nadie tapa la agenda con reservas fantasma.
    @Query("select r.clienteWhatsapp from ReservaJpaEntity r where r.tenantId = :tenantId "
            + "and " + OCUPA + "and r.inicio > :ahora")
    List<String> clienteWhatsappDeActivasFuturas(@Param("tenantId") Long tenantId,
            @Param("ahora") LocalDateTime ahora);

    // --- Cleanup (hard delete) de reservas pasadas: no hay histórico de turnos (decisión de producto). ---
    @Modifying
    @Query(value = "DELETE FROM reservas WHERE fin < :cutoff", nativeQuery = true)
    int deleteByFinBefore(@Param("cutoff") LocalDateTime cutoff);

    // --- Job de señas: pasa a CANCELADO las pendientes ya vencidas (cosmético; el slot ya se liberó
    // solo por el predicado OCUPA). Nativo y sin scope de tenant: barre todos los tenants. ---
    @Modifying
    @Query(value = "UPDATE reservas SET estado = 'CANCELADO' "
            + "WHERE estado = 'PENDIENTE' AND active = TRUE AND expira_en < :ahora", nativeQuery = true)
    int cancelarPendientesVencidas(@Param("ahora") LocalDateTime ahora);
}
