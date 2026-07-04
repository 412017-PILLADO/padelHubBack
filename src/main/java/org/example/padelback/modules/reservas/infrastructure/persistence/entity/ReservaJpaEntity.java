package org.example.padelback.modules.reservas.infrastructure.persistence.entity;

import java.time.LocalDateTime;

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
import org.example.padelback.modules.reservas.domain.model.ReservaEstado;

/** Fuente de verdad de los turnos. Lleva su propia {@code duracionMinutos} (turnos variables). */
@Entity
@Table(name = "reservas", indexes = {
        @Index(name = "idx_reservas_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_reservas_cancha_inicio", columnList = "tenant_id, cancha_id, inicio"),
        @Index(name = "idx_reservas_complejo_inicio", columnList = "tenant_id, complejo_id, inicio")
})
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ReservaJpaEntity extends BaseJpaEntity {

    @Column(name = "complejo_id", nullable = false)
    private Long complejoId;

    @Column(name = "cancha_id", nullable = false)
    private Long canchaId;

    @Column(name = "cliente_nombre", nullable = false, length = 150)
    private String clienteNombre;

    @Column(name = "cliente_whatsapp", length = 40)
    private String clienteWhatsapp;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fin;

    @Column(name = "duracion_minutos", nullable = false)
    private int duracionMinutos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservaEstado estado;

    /**
     * Solo para reservas PENDIENTE (seña): momento en que dejan de retener la cancha. CONFIRMADO y
     * CANCELADO lo llevan en null. Una reserva "ocupa" el slot si {@code expiraEn IS NULL} (confirmada)
     * o {@code expiraEn > ahora} (pendiente todavía vigente).
     */
    @Column(name = "expira_en")
    private LocalDateTime expiraEn;

    @Column(name = "ip", length = 45)
    private String ip;
}
