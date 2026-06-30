package org.example.padelback.modules.reservas.infrastructure.persistence.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.example.padelback.infrastructure.persistence.entity.BaseJpaEntity;

/**
 * Excepcion puntual que bloquea disponibilidad. Si {@code canchaId} es null, aplica a TODO el
 * complejo (ej. feriado, evento); si tiene valor, bloquea solo esa cancha (ej. mantenimiento).
 */
@Entity
@Table(name = "bloqueos", indexes = {
        @Index(name = "idx_bloqueos_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_bloqueos_complejo_rango", columnList = "tenant_id, complejo_id, fecha_hora_desde")
})
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BloqueoJpaEntity extends BaseJpaEntity {

    @Column(name = "complejo_id", nullable = false)
    private Long complejoId;

    /** null = bloquea todas las canchas del complejo. */
    @Column(name = "cancha_id")
    private Long canchaId;

    @Column(name = "fecha_hora_desde", nullable = false)
    private LocalDateTime fechaHoraDesde;

    @Column(name = "fecha_hora_hasta", nullable = false)
    private LocalDateTime fechaHoraHasta;

    @Column(length = 255)
    private String motivo;
}
