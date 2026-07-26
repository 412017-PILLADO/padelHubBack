package org.example.padelback.modules.reservas.infrastructure.persistence.entity;

import java.time.LocalDate;

import org.example.padelback.infrastructure.persistence.entity.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Solicitud de arrepentimiento (Res. 424/2020): el consumidor revoca sin registro previo y recibe
 * un {@code codigo} de vuelta; el dueño la gestiona desde el panel (cancela reserva / devuelve
 * seña por fuera de este módulo).
 */
@Entity
@Table(name = "arrepentimientos", indexes = {
        @Index(name = "idx_arrep_tenant_gestionado", columnList = "tenant_id, gestionado")
})
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ArrepentimientoJpaEntity extends BaseJpaEntity {

    @Column(nullable = false, length = 12)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 40)
    private String whatsapp;

    @Column(columnDefinition = "TEXT")
    private String detalle;

    @Column(name = "reserva_fecha")
    private LocalDate reservaFecha;

    @Column(nullable = false)
    private boolean gestionado;
}
