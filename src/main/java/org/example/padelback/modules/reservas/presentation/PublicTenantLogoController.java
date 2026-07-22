package org.example.padelback.modules.reservas.presentation;

import org.example.padelback.domain.port.TenantProvider;
import org.example.padelback.modules.tenant.infrastructure.PublicTenantResolver;
import org.example.padelback.modules.tenant.infrastructure.persistence.TenantLogoStore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sirve el logo del tenant (bytes guardados en la base) para la página pública. El tenant se toma
 * del query param {@code tenant} (slug) — el {@code <img>} no manda header X-Tenant y en prod
 * front/back viven en orígenes distintos, así que no se puede resolver por host. Si no viene el
 * param, cae al tenant del contexto (resuelto por host en accesos same-origin / dev).
 */
@RestController
@RequestMapping("/public/tenant")
@RequiredArgsConstructor
public class PublicTenantLogoController {

    private final TenantProvider tenantProvider;
    private final PublicTenantResolver tenantResolver;
    private final TenantLogoStore logoStore;

    @GetMapping("/logo")
    public ResponseEntity<byte[]> logo(@RequestParam(required = false) String tenant) {
        Long tenantId = (tenant != null && !tenant.isBlank())
                ? tenantResolver.resolve(tenant, null).orElse(null)
                : tenantProvider.currentTenantId().orElse(null);
        if (tenantId == null) {
            return ResponseEntity.notFound().build();
        }
        return logoStore.load(tenantId)
                .map(l -> ResponseEntity.ok()
                        .contentType(parse(l.contentType()))
                        .cacheControl(CacheControl.maxAge(java.time.Duration.ofHours(1)).cachePublic())
                        .body(l.bytes()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static MediaType parse(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (RuntimeException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
