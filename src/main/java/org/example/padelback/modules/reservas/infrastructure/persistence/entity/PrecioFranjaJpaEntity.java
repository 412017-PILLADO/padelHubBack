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
 * Franja horaria con ajuste porcentual de precio, GENERAL del complejo: el porcentaje se aplica
 * sobre el precio original (general o de cada cancha según el modo), así todas las canchas
 * mantienen su precio relativo. Negativo = descuento, positivo = recargo. Aplica todos los días
 * por igual (no es por día de semana, a diferencia de {@link HorarioComplejoJpaEntity}). Un turno
 * la paga si su hora de inicio cae dentro de {@code [horaDesde, horaHasta)}.
 */
@Entity
@Table(name = "precio_franjas", indexes = {
        @Index(name = "idx_precio_franjas_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_precio_franjas_complejo", columnList = "tenant_id, complejo_id")
})
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PrecioFranjaJpaEntity extends BaseJpaEntity {

    @Column(name = "complejo_id", nullable = false)
    private Long complejoId;

    @Column(name = "hora_desde", nullable = false)
    private LocalTime horaDesde;

    @Column(name = "hora_hasta", nullable = false)
    private LocalTime horaHasta;

    @Column(name = "ajuste_porcentaje", nullable = false)
    private Integer ajustePorcentaje;
}
