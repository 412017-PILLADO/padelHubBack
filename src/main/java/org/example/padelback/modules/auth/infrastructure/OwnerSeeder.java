package org.example.padelback.modules.auth.infrastructure;

import org.example.padelback.infrastructure.tenancy.TenantContext;
import org.example.padelback.modules.auth.domain.model.UsuarioRol;
import org.example.padelback.modules.auth.infrastructure.persistence.entity.UsuarioJpaEntity;
import org.example.padelback.modules.auth.infrastructure.persistence.repository.UsuarioJpaRepository;
import org.example.padelback.modules.tenant.infrastructure.persistence.repository.TenantDominioJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Siembra (idempotente) el usuario OWNER del tenant para entrar al panel. Resuelve el tenant por
 * el host seedeado 'localhost'.
 *
 * <p><b>Contraseña — seguro para prod:</b>
 * <ul>
 *   <li><b>dev/tests</b> (perfil != prod): usa el default {@code padel123} si no hay otro.</li>
 *   <li><b>prod</b> (perfil {@code prod}): usa SOLO {@code padel.owner.password} (env
 *       {@code PADEL_OWNER_PASSWORD}). Si no está seteada, <b>no crea nada</b> — así no queda un owner
 *       con contraseña conocida. Email configurable por {@code padel.owner.email}.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class OwnerSeeder implements CommandLineRunner {

    private static final String DEV_PASSWORD = "padel123";

    private final TenantDominioJpaRepository dominioRepo;
    private final UsuarioJpaRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;
    private final Environment springEnv;

    @Value("${padel.owner.email:owner@padelhub.com}")
    private String ownerEmail;
    @Value("${padel.owner.password:}")
    private String ownerPasswordProp;

    @Override
    @Transactional
    public void run(String... args) {
        boolean prod = springEnv.acceptsProfiles(Profiles.of("prod"));
        String password = !ownerPasswordProp.isBlank() ? ownerPasswordProp : (prod ? "" : DEV_PASSWORD);
        if (password.isBlank()) {
            return; // prod sin PADEL_OWNER_PASSWORD: no se siembra ningún owner.
        }

        dominioRepo.findByHost("localhost").ifPresent(dom -> {
            Long tenantId = dom.getTenantId();
            if (usuarioRepo.findByTenantIdAndEmailAndActiveTrue(tenantId, ownerEmail).isEmpty()) {
                TenantContext.runAs(tenantId, () -> {
                    UsuarioJpaEntity owner = UsuarioJpaEntity.builder()
                            .tenantId(tenantId)
                            .email(ownerEmail)
                            .passwordHash(passwordEncoder.encode(password))
                            .rol(UsuarioRol.OWNER)
                            .estado("ACTIVO")
                            .build();
                    usuarioRepo.save(owner);
                    return null;
                });
            }
        });
    }
}
