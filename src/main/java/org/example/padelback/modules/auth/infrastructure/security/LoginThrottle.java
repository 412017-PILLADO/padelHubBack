package org.example.padelback.modules.auth.infrastructure.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Freno de fuerza bruta para el login de owners ({@code /api/v1/auth/login}), multi-tenant. Combina
 * dos dimensiones (mismo patrón que {@code PlatformLoginThrottle}):
 * <ul>
 *   <li>Por (tenant+email): {@value #MAX_FAILS_CREDENCIAL} fallos → bloquea esa cuenta {@link #LOCK}.
 *   <li>Por IP: {@value #MAX_FAILS_IP} fallos (probando cualquier cuenta) → bloquea esa IP
 *       {@link #LOCK}, para que alguien no pruebe credenciales contra muchos tenants/emails distintos
 *       desde el mismo origen sin nunca llegar al tope por cuenta.
 * </ul>
 * En memoria (una instancia): para multi-instancia habría que mover el estado a un store compartido.
 * Un login exitoso limpia el contador de esa cuenta (la IP no se limpia: agrupa varias cuentas).
 */
@Component
public class LoginThrottle {

    static final int MAX_FAILS_CREDENCIAL = 5;
    static final int MAX_FAILS_IP = 20;
    static final Duration LOCK = Duration.ofMinutes(15);

    private final Map<String, Estado> porCredencial = new ConcurrentHashMap<>();
    private final Map<String, Estado> porIp = new ConcurrentHashMap<>();

    private static final class Estado {
        int fails;
        Instant lockedUntil;
    }

    /** Lanza 429 si la cuenta (tenant+email) o la IP están bloqueadas. */
    public void assertNotLocked(Long tenantId, String email, String ip) {
        checkLocked(porCredencial, credencialKey(tenantId, email));
        checkLocked(porIp, ipKey(ip));
    }

    /** Registra un intento fallido en ambas dimensiones; al llegar al tope, bloquea. */
    public void recordFailure(Long tenantId, String email, String ip) {
        recordFailureIn(porCredencial, credencialKey(tenantId, email), MAX_FAILS_CREDENCIAL);
        recordFailureIn(porIp, ipKey(ip), MAX_FAILS_IP);
    }

    /** Limpia el estado de la cuenta tras un login exitoso. */
    public void recordSuccess(Long tenantId, String email) {
        porCredencial.remove(credencialKey(tenantId, email));
    }

    private void checkLocked(Map<String, Estado> store, String key) {
        Estado e = store.get(key);
        if (e == null) {
            return;
        }
        synchronized (e) {
            if (e.lockedUntil != null && Instant.now().isBefore(e.lockedUntil)) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Demasiados intentos fallidos. Esperá unos minutos e intentá de nuevo.");
            }
        }
    }

    private void recordFailureIn(Map<String, Estado> store, String key, int maxFails) {
        Estado e = store.computeIfAbsent(key, k -> new Estado());
        synchronized (e) {
            // Si el bloqueo ya venció, arrancamos de cero antes de contar este fallo.
            if (e.lockedUntil != null && !Instant.now().isBefore(e.lockedUntil)) {
                e.fails = 0;
                e.lockedUntil = null;
            }
            e.fails++;
            if (e.fails >= maxFails) {
                e.lockedUntil = Instant.now().plus(LOCK);
            }
        }
    }

    private static String credencialKey(Long tenantId, String email) {
        return tenantId + "|" + (email == null ? "" : email.trim().toLowerCase());
    }

    private static String ipKey(String ip) {
        return ip == null ? "" : ip.trim();
    }
}
