package org.example.padelback.modules.auth.infrastructure.security;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.example.padelback.modules.auth.domain.model.UsuarioRol;
import org.springframework.stereotype.Component;

/**
 * Códigos de un solo uso para pasar una sesión del apex al subdominio del club.
 *
 * <p>El JWT vive en {@code localStorage}, que es por origen: un token obtenido en
 * {@code padel-hub.com.ar} es invisible para {@code demo.padel-hub.com.ar}. Este store emite un
 * código efímero que el apex manda en la URL del redirect y que el subdominio canjea por el JWT.
 * Mandar el JWT crudo en la URL haría lo mismo, pero una URL con JWT queda en el historial, en lo
 * que el usuario pegue en un chat y en cualquier extensión que lea la barra de direcciones — y
 * sigue siendo válida durante horas. El código, para cuando llega a esos lugares, ya no sirve.
 *
 * <p>En memoria (una instancia): para multi-instancia habría que mover el estado a un store
 * compartido, misma limitación que {@link LoginThrottle}.
 */
@Component
public class CodigoIngresoStore {

    /** Alcanza para un redirect y para nada más. */
    static final Duration TTL = Duration.ofSeconds(60);

    private static final int BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    /** Lo que el código representa: quién es y de qué club, para poder emitir el JWT en el canje. */
    public record Entrada(Long tenantId, String email, UsuarioRol rol) {}

    private record Guardado(Entrada entrada, Instant vence) {}

    private final Map<String, Guardado> codigos = new ConcurrentHashMap<>();
    private final Clock clock;

    public CodigoIngresoStore() {
        this(Clock.systemUTC());
    }

    /** Para los tests: reloj inyectable, así el TTL se prueba sin dormir. */
    CodigoIngresoStore(Clock clock) {
        this.clock = clock;
    }

    public String emitir(Long tenantId, String email, UsuarioRol rol) {
        purgarVencidos();
        byte[] bytes = new byte[BYTES];
        RANDOM.nextBytes(bytes);
        String codigo = ENCODER.encodeToString(bytes);
        codigos.put(codigo, new Guardado(new Entrada(tenantId, email, rol), clock.instant().plus(TTL)));
        return codigo;
    }

    /**
     * Un solo uso: saca la entrada del mapa y RECIÉN DESPUÉS mira si venció. Al revés, un código
     * vencido quedaría en el mapa para siempre.
     */
    public Optional<Entrada> consumir(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        Guardado guardado = codigos.remove(codigo);
        if (guardado == null || !clock.instant().isBefore(guardado.vence())) {
            return Optional.empty();
        }
        return Optional.of(guardado.entrada());
    }

    /** Purga perezosa en cada emisión: el mapa nunca pasa de un puñado de entradas. */
    private void purgarVencidos() {
        Instant ahora = clock.instant();
        codigos.values().removeIf(g -> !ahora.isBefore(g.vence()));
    }
}
