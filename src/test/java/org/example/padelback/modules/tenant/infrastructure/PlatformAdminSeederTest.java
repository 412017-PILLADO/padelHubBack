package org.example.padelback.modules.tenant.infrastructure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.example.padelback.modules.tenant.infrastructure.persistence.PlatformAdminStore;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Política de siembra del super-admin: en prod NO se siembra si falta la password por env; en dev se
 * usa el default. Espeja la política del OwnerSeeder y protege el arranque en producción.
 */
class PlatformAdminSeederTest {

    private PlatformAdminSeeder seeder(PlatformAdminStore store, PasswordEncoder encoder, Environment env,
                                       String passwordProp) {
        PlatformAdminSeeder s = new PlatformAdminSeeder(store, encoder, env);
        ReflectionTestUtils.setField(s, "adminEmail", "admin@padelhub.com");
        ReflectionTestUtils.setField(s, "adminPasswordProp", passwordProp);
        return s;
    }

    @Test
    void enProdSinPasswordNoSiembra() {
        PlatformAdminStore store = mock(PlatformAdminStore.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        Environment env = mock(Environment.class);
        when(env.acceptsProfiles(any(Profiles.class))).thenReturn(true); // perfil prod

        seeder(store, encoder, env, "").run();

        verify(store, never()).create(anyString(), anyString());
    }

    @Test
    void enProdConPasswordSiembra() {
        PlatformAdminStore store = mock(PlatformAdminStore.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        Environment env = mock(Environment.class);
        when(env.acceptsProfiles(any(Profiles.class))).thenReturn(true); // perfil prod
        when(store.existsByEmail(anyString())).thenReturn(false);
        when(encoder.encode("prod-secret")).thenReturn("HASH");

        seeder(store, encoder, env, "prod-secret").run();

        verify(store).create(eq("admin@padelhub.com"), eq("HASH"));
    }

    @Test
    void enDevSiembraConDefault() {
        PlatformAdminStore store = mock(PlatformAdminStore.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        Environment env = mock(Environment.class);
        when(env.acceptsProfiles(any(Profiles.class))).thenReturn(false); // no prod
        when(store.existsByEmail(anyString())).thenReturn(false);
        when(encoder.encode(anyString())).thenReturn("HASH");

        seeder(store, encoder, env, "").run();

        verify(store).create(eq("admin@padelhub.com"), eq("HASH"));
    }
}
