package org.example.padelback.modules.tenant.presentation;

import org.example.padelback.modules.tenant.domain.model.TenantStatus;
import org.example.padelback.modules.tenant.infrastructure.HostTenantResolver;
import org.example.padelback.modules.tenant.infrastructure.persistence.repository.TenantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autoriza (o no) que Caddy emita un certificado TLS para un dominio, vía su {@code on_demand_tls
 * ask}. Caddy pega un GET con {@code ?domain=<host>} antes de pedirle el certificado a Let's
 * Encrypt: 200 = adelante, cualquier otra cosa = no.
 *
 * <p><b>Por qué hace falta.</b> Los clubes son subdominios que nacen con el tiempo, así que no se
 * puede tener el certificado listo de antemano — de ahí el "a demanda". Pero sin este control,
 * cualquiera que apunte {@code loquesea.com} al IP del servidor haría que Caddy le pida un
 * certificado a Let's Encrypt, y eso agota la cuota semanal de emisiones en horas y deja a los
 * clubes reales sin poder renovar.
 *
 * <p><b>Qué se considera válido.</b> El apex y su {@code www} (la landing de venta), un subdominio
 * cuyo primer segmento sea el slug de un club ACTIVO, y cualquier host cargado a mano en
 * {@code tenant_dominios} (los dominios propios de un club). Un club INACTIVE no califica: si está
 * suspendido, tampoco sirve su landing.
 */
@RestController
@RequiredArgsConstructor
public class TlsAskController {

    private final TenantJpaRepository tenantRepo;
    private final HostTenantResolver hostResolver;

    /** Dominio base del producto, ej. {@code padel-hub.com.ar}. Vacío = sólo se aceptan hosts de
     *  {@code tenant_dominios} (útil en dev, donde no hay dominio de verdad). */
    @Value("${padel.public.base-domain:}")
    private String baseDomain;

    @GetMapping("/public/tenant/existe")
    public ResponseEntity<Void> existe(@RequestParam("domain") String domain) {
        String host = domain == null ? "" : domain.trim().toLowerCase();
        if (host.isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return permitido(host)
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    private boolean permitido(String host) {
        String base = baseDomain == null ? "" : baseDomain.trim().toLowerCase();
        if (!base.isBlank()) {
            if (host.equals(base) || host.equals("www." + base)) {
                return true; // la landing de venta
            }
            if (host.endsWith("." + base)) {
                String slug = host.substring(0, host.length() - base.length() - 1);
                // Sólo el primer nivel: `a.b.padel-hub.com.ar` no es el slug "a.b".
                if (!slug.isBlank() && !slug.contains(".")) {
                    return tenantRepo.findBySlugAndStatus(slug, TenantStatus.ACTIVE).isPresent();
                }
            }
        }
        // Dominio propio del club, cargado en tenant_dominios.
        return hostResolver.resolve(host).isPresent();
    }
}
