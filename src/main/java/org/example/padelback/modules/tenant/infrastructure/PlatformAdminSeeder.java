package org.example.padelback.modules.tenant.infrastructure;

import org.example.padelback.modules.tenant.infrastructure.persistence.PlatformAdminStore;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Siembra (idempotente) el super-admin de plataforma para entrar al panel de dev y dar de alta clubes.
 * Misma política de contraseña que {@link org.example.padelback.modules.auth.infrastructure.OwnerSeeder}:
 * dev usa el default; prod usa SOLO {@code padel.platform.admin-password} (env
 * {@code PADEL_PLATFORM_ADMIN_PASSWORD}) y si no está, no siembra nada.
 */
@Component
@RequiredArgsConstructor
public class PlatformAdminSeeder implements CommandLineRunner {

    private static final String DEV_PASSWORD = "superadmin123";

    private final PlatformAdminStore store;
    private final PasswordEncoder passwordEncoder;
    private final Environment springEnv;

    @Value("${padel.platform.admin-email:admin@padelhub.com}")
    private String adminEmail;
    @Value("${padel.platform.admin-password:}")
    private String adminPasswordProp;

    @Override
    public void run(String... args) {
        boolean prod = springEnv.acceptsProfiles(Profiles.of("prod"));
        String password = !adminPasswordProp.isBlank() ? adminPasswordProp : (prod ? "" : DEV_PASSWORD);
        if (password.isBlank()) {
            return; // prod sin PADEL_PLATFORM_ADMIN_PASSWORD: no se siembra ningún super-admin.
        }
        String email = adminEmail.trim().toLowerCase();
        if (!store.existsByEmail(email)) {
            store.create(email, passwordEncoder.encode(password));
        }
    }
}
