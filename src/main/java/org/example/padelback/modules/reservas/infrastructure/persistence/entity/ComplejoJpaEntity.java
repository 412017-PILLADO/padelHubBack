package org.example.padelback.modules.reservas.infrastructure.persistence.entity;

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
import org.example.padelback.modules.reservas.domain.model.ComplejoEstado;

/**
 * El complejo (local) de padel. Equivale a la "sucursal" de barber, pero la config de agenda vive
 * aca: {@code pasoMinutos} (granularidad de los inicios de turno), {@code duracionesPermitidas}
 * (CSV de duraciones que el cliente puede elegir, ej. "60,90,120") y {@code duracionDefault}.
 */
@Entity
@Table(name = "complejos", indexes = @Index(name = "idx_complejos_tenant_id", columnList = "tenant_id"))
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ComplejoJpaEntity extends BaseJpaEntity {

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 255)
    private String direccion;

    @Column(length = 40)
    private String telefono;

    @Column(length = 40)
    private String whatsapp;

    @Column(name = "mapa_url", length = 500)
    private String mapaUrl;

    @Column(length = 100)
    private String instagram;

    /** Granularidad de los horarios de inicio ofrecidos (ej. 30 => 18:00, 18:30, 19:00...). */
    @Column(name = "paso_minutos", nullable = false)
    private int pasoMinutos;

    /** Duraciones elegibles por el cliente, CSV de minutos (ej. "60,90,120"). */
    @Column(name = "duraciones_permitidas", nullable = false, length = 60)
    private String duracionesPermitidas;

    @Column(name = "duracion_default", nullable = false)
    private int duracionDefault;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComplejoEstado estado;
}
