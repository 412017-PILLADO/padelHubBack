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
import java.math.BigDecimal;

import org.example.padelback.infrastructure.persistence.entity.BaseJpaEntity;
import org.example.padelback.modules.reservas.domain.model.ComplejoEstado;
import org.example.padelback.modules.reservas.domain.model.PrecioModo;

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

    /**
     * El turno principal ({@code duracionDefault}) define la grilla de inicios. Si está en
     * {@code false}, el cliente solo puede reservar ese turno principal (60/120 quedan ocultos);
     * si está en {@code true} se ofrecen todas las {@code duracionesPermitidas}, ancladas a la
     * grilla del turno principal.
     */
    @Column(name = "permitir_otras_duraciones", nullable = false)
    private boolean permitirOtrasDuraciones;

    /** GENERAL = un precio por hora para todo el complejo; POR_CANCHA = el de cada cancha. */
    @Enumerated(EnumType.STRING)
    @Column(name = "precio_modo", nullable = false, length = 20)
    private PrecioModo precioModo;

    /** Precio por hora cuando {@link #precioModo} es GENERAL (se ignora el precio de cada cancha). */
    @Column(name = "precio_hora_general", precision = 10, scale = 2)
    private BigDecimal precioHoraGeneral;

    /**
     * Módulo de señas: si está en {@code true}, las reservas nuevas nacen PENDIENTE (a la espera de
     * que el dueño valide la seña) y retienen la cancha durante la ventana de pago; si está en
     * {@code false} nacen CONFIRMADO directo (comportamiento por defecto).
     */
    @Column(name = "requiere_sena", nullable = false)
    private boolean requiereSena;

    /** Monto informativo de la seña que se le muestra al cliente (no se cobra en la app). */
    @Column(name = "sena_monto", precision = 10, scale = 2)
    private BigDecimal senaMonto;

    /** Alias / CBU / CVU al que el cliente transfiere la seña (se muestra con botón para copiar). */
    @Column(name = "sena_alias", length = 100)
    private String senaAlias;

    /**
     * Si está en {@code true}, el sistema asigna una cancha disponible automáticamente (la menos
     * cargada) y la landing no le muestra al cliente el paso de elegir cancha.
     */
    @Column(name = "autoasignacion", nullable = false)
    private boolean autoasignacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComplejoEstado estado;
}
