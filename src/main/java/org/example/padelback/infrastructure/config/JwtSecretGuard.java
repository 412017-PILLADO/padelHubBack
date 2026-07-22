package org.example.padelback.infrastructure.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Fail-fast del secreto JWT en producción. Si el perfil activo es {@code prod} y {@code padel.jwt.secret}
 * quedó en el default de dev o es demasiado corto para HS256 (&lt; 32 chars), aborta el arranque en vez
 * de firmar tokens con un secreto conocido/débil. En dev/tests no valida nada (default cómodo).
 *
 * <p>Sigue el patrón de {@code OwnerSeeder}, que también endurece su comportamiento leyendo el perfil
 * activo del {@link Environment}. Acá se hace en {@code @PostConstruct} para cortar el arranque temprano.
 */
@Component
@RequiredArgsConstructor
public class JwtSecretGuard {

    /** Debe coincidir con el default de dev de {@code padel.jwt.secret} en application.yml. */
    private static final String DEV_DEFAULT = "padel-dev-secret-change-me-please-min-32-chars-aaaa";
    private static final int MIN_LEN = 32;

    private final Environment springEnv;
    private final JwtProperties jwt;

    @PostConstruct
    void verificar() {
        if (!springEnv.acceptsProfiles(Profiles.of("prod"))) {
            return; // dev/tests: se permite el default.
        }
        String secret = jwt.secret();
        if (secret == null || secret.isBlank() || secret.equals(DEV_DEFAULT) || secret.length() < MIN_LEN) {
            throw new IllegalStateException(
                    "Configurá PADEL_JWT_SECRET con un secreto propio de al menos " + MIN_LEN
                            + " caracteres antes de arrancar en prod (el default de dev no es válido).");
        }
    }
}
