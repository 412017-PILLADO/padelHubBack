package org.example.padelback.modules.reservas.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.example.padelback.modules.reservas.domain.model.CanchaEstado;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.CanchaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CanchaJpaRepository extends JpaRepository<CanchaJpaEntity, Long> {

    List<CanchaJpaEntity> findByTenantIdAndComplejoIdAndEstadoAndActiveTrueOrderByOrdenAsc(
            Long tenantId, Long complejoId, CanchaEstado estado);

    /** Todas las canchas activas del tenant (para resolver nombres en el panel de turnos). */
    List<CanchaJpaEntity> findByTenantIdAndActiveTrue(Long tenantId);

    Optional<CanchaJpaEntity> findByTenantIdAndIdAndActiveTrue(Long tenantId, Long id);

    /** Lock pesimista para anti-doble-reserva: serializa las reservas concurrentes sobre la cancha. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CanchaJpaEntity c where c.tenantId = :tenantId and c.id = :id and c.active = true")
    Optional<CanchaJpaEntity> lockByTenantIdAndId(@Param("tenantId") Long tenantId, @Param("id") Long id);
}
