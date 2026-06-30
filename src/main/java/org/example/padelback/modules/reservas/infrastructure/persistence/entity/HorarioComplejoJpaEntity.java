package org.example.padelback.modules.reservas.infrastructure.persistence.entity;

import java.time.LocalTime;

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
 * Franja horaria de apertura del complejo, compartida por todas sus canchas. El cierre al mediodia
 * se modela como hueco: dos filas el mismo dia (ej. 08:00-13:00 y 16:00-23:00).
 */
@Entity
@Table(name = "horarios_complejo", indexes = {
        @Index(name = "idx_horarios_complejo_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_horarios_complejo_dia", columnList = "tenant_id, complejo_id, dia_semana")
})
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class HorarioComplejoJpaEntity extends BaseJpaEntity {

    @Column(name = "complejo_id", nullable = false)
    private Long complejoId;

    // 0=LUNES ... 6=DOMINGO (ver enum DiaSemana)
    @Column(name = "dia_semana", nullable = false)
    private int diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;
}
