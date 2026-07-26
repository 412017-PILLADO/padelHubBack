package org.example.padelback.infrastructure.web;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.example.padelback.infrastructure.config.AntiAbuseProperties;
import org.springframework.stereotype.Component;

/**
 * Rate limit in-memory por IP para escrituras públicas anónimas (ventana fija). Una instancia:
 * suficiente para el tamaño actual; multi-instancia requeriría store compartido (mismo trade-off
 * documentado en PlatformLoginThrottle). El anti-abuso de reservas (por DB) queda intacto.
 */
@Component
public class PublicWriteThrottle {

    private final AntiAbuseProperties props;
    private final Map<String, Estado> porIp = new ConcurrentHashMap<>();

    private static final class Estado {
        int count;
        Instant ventanaDesde;
    }

    public PublicWriteThrottle(AntiAbuseProperties props) {
        this.props = props;
    }

    public boolean permitir(String ip, Instant ahora) {
        Estado e = porIp.computeIfAbsent(ip == null ? "" : ip.trim(), k -> new Estado());
        synchronized (e) {
            if (e.ventanaDesde == null || ahora.isAfter(e.ventanaDesde.plus(props.publico().ventana()))) {
                e.ventanaDesde = ahora;
                e.count = 0;
            }
            e.count++;
            return e.count <= props.publico().maxPorVentana();
        }
    }
}
