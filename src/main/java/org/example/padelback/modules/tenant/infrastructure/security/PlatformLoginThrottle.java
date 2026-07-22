package org.example.padelback.modules.tenant.infrastructure.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Freno de fuerza bruta para el login de plataforma. Combina dos dimensiones para evitar un
 * lockout-DoS: si el bloqueo fuera solo por email (como antes), cualquiera podría tirar
 * {@value #MAX_FAILS_EMAIL_IP} intentos fallidos con el email del super-admin real desde una IP
 * ajena y dejarlo afuera {@link #LOCK} sin saber la clave.
 * <ul>
 *   <li>Por (email+IP): {@value #MAX_FAILS_EMAIL_IP} fallos → bloquea esa combinación {@link #LOCK}.
 *       El atacante necesita pegarle desde la MISMA IP que usaría el admin real para bloquearlo.
 *   <li>Por IP sola: {@value #MAX_FAILS_IP} fallos (probando cualquier email) → bloquea esa IP
 *       {@link #LOCK}, para frenar fuerza bruta / credential stuffing desde un mismo origen.
 * </ul>
 * Trade-off: un atacante que rote de IP en cada intento no cae en ninguna de las dos dimensiones;
 * mitigar eso requiere rate-limit en el borde (WAF/proxy), fuera de alcance acá. En memoria (una
 * instancia): el panel de super-admin es de bajo volumen; para multi-instancia habría que mover el
 * estado a un store compartido. Un login exitoso limpia el contador de esa combinación email+IP.
 */
@Component
public class PlatformLoginThrottle {

    static final int MAX_FAILS_EMAIL_IP = 5;
    static final int MAX_FAILS_IP = 20;
    static final Duration LOCK = Duration.ofMinutes(15);

    private final Map<String, Estado> porEmailIp = new ConcurrentHashMap<>();
    private final Map<String, Estado> porIp = new ConcurrentHashMap<>();

    private static final class Estado {
        int fails;
        Instant lockedUntil;
    }

    /** Lanza 429 si la combinación email+IP o la IP sola están bloqueadas. */
    public void assertNotLocked(String email, String ip) {
        checkLocked(porEmailIp, emailIpKey(email, ip));
        checkLocked(porIp, ipKey(ip));
    }

    /** Registra un intento fallido en ambas dimensiones; al llegar al tope, bloquea. */
    public void recordFailure(String email, String ip) {
        recordFailureIn(porEmailIp, emailIpKey(email, ip), MAX_FAILS_EMAIL_IP);
        recordFailureIn(porIp, ipKey(ip), MAX_FAILS_IP);
    }

    /** Limpia el estado de esa combinación email+IP tras un login exitoso. */
    public void recordSuccess(String email, String ip) {
        porEmailIp.remove(emailIpKey(email, ip));
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

    private static String emailIpKey(String email, String ip) {
        return (email == null ? "" : email.trim().toLowerCase()) + "|" + ipKey(ip);
    }

    private static String ipKey(String ip) {
        return ip == null ? "" : ip.trim();
    }
}
