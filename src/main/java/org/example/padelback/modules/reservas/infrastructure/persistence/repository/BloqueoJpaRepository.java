package org.example.padelback.modules.reservas.infrastructure.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.example.padelback.modules.reservas.infrastructure.persistence.entity.BloqueoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BloqueoJpaRepository extends JpaRepository<BloqueoJpaEntity, Long> {

    // Bloqueos del complejo que solapan [desde, hasta): fechaHoraDesde < hasta AND fechaHoraHasta > desde.
    // Incluye los de cancha específica (cancha_id != null) y los del complejo entero (cancha_id null).
    List<BloqueoJpaEntity> findByTenantIdAndComplejoIdAndActiveTrueAndFechaHoraDesdeLessThanAndFechaHoraHastaGreaterThan(
            Long tenantId, Long complejoId, LocalDateTime hasta, LocalDateTime desde);

    List<BloqueoJpaEntity> findByTenantIdAndComplejoIdAndActiveTrueAndFechaHoraHastaGreaterThanEqualOrderByFechaHoraDesdeAsc(
            Long tenantId, Long complejoId, LocalDateTime desde);

    Optional<BloqueoJpaEntity> findByTenantIdAndIdAndActiveTrue(Long tenantId, Long id);
}
