package org.example.padelback.modules.reservas.infrastructure.persistence.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.example.padelback.infrastructure.persistence.entity.BaseJpaEntity;
import org.example.padelback.modules.reservas.domain.model.CanchaEstado;
import org.example.padelback.modules.reservas.domain.model.TipoPared;

/**
 * Una cancha del complejo. Es el recurso reservable (equivale al "barbero" de barber): un slot
 * esta disponible si al menos una cancha esta libre. Las canchas comparten el horario del complejo.
 */
@Entity
@Table(name = "canchas", indexes = {
        @Index(name = "idx_canchas_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_canchas_complejo", columnList = "tenant_id, complejo_id")
})
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CanchaJpaEntity extends BaseJpaEntity {

    @Column(name = "complejo_id", nullable = false)
    private Long complejoId;

    @Column(nullable = false, length = 150)
    private String nombre;

    /** Orden de visualizacion en la grilla. */
    @Column(nullable = false)
    private int orden;

    @Column(nullable = false)
    private boolean techada;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pared", nullable = false, length = 20)
    private TipoPared tipoPared;

    @Column(name = "precio_hora", precision = 10, scale = 2)
    private BigDecimal precioHora;

    /** Color para la UI (hex), diferencia visual de la cancha en la grilla/panel. */
    @Column(length = 20)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CanchaEstado estado;
}
