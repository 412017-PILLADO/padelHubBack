package org.example.padelback.infrastructure.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Aislamiento por tenant en DOS capas:
 * <ul>
 *   <li><b>Escritura</b>: {@link TenantEntityListener} inyecta/valida {@code tenant_id} en persist/update.</li>
 *   <li><b>Lectura</b>: el {@code tenantFilter} de Hibernate (definido acá, heredado por todas las entidades
 *       de negocio) agrega {@code tenant_id = :tenantId} a las queries. Es la <b>red de seguridad</b> del §9:
 *       además de los {@code ...AndTenantId} explícitos, si una query se olvida del filtro, este lo aplica igual.
 *       Lo activa {@code TenantFilterAspect} por transacción desde el {@code TenantContext}.</li>
 * </ul>
 */
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@SuperBuilder
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners({ AuditingEntityListener.class, TenantEntityListener.class })
public abstract class BaseJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false, length = 120)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", nullable = false, length = 120)
    private String updatedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public void markDeleted(String actor) {
        this.active = false;
        this.deletedAt = Instant.now();
        this.updatedBy = actor;
    }

    public boolean isDeleted() {
        return deletedAt != null || !active;
    }
}
