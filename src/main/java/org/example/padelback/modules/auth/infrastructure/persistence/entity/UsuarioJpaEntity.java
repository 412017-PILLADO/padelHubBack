package org.example.padelback.modules.auth.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.example.padelback.infrastructure.persistence.entity.BaseJpaEntity;
import org.example.padelback.modules.auth.domain.model.UsuarioRol;

@Entity
@Table(
        name = "usuarios",
        uniqueConstraints = @UniqueConstraint(name = "idx_usuarios_tenant_email", columnNames = {"tenant_id", "email"}),
        indexes = @Index(name = "idx_usuarios_tenant_id", columnList = "tenant_id")
)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UsuarioJpaEntity extends BaseJpaEntity {

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UsuarioRol rol;

    @Column(nullable = false, length = 20)
    private String estado;
}
