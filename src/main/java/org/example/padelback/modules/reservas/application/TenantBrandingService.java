package org.example.padelback.modules.reservas.application;

import java.time.Instant;

import org.example.padelback.domain.port.TenantProvider;
import org.example.padelback.modules.tenant.infrastructure.persistence.TenantLogoStore;
import org.example.padelback.modules.tenant.infrastructure.persistence.entity.TenantJpaEntity;
import org.example.padelback.modules.tenant.infrastructure.persistence.repository.TenantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Marca del tenant editable desde el panel: color primario, fuente y logo (bytes en base). El logo
 * se sirve por {@code /public/tenant/logo?tenant=<slug>} cuando hay bytes; si no, cae a la URL
 * externa configurada. Todo opera sobre el tenant del contexto (OWNER logueado).
 */
@Service
@RequiredArgsConstructor
public class TenantBrandingService {

    private static final String LOGO_ENDPOINT = "/public/tenant/logo";

    private final TenantProvider tenantProvider;
    private final TenantJpaRepository tenantRepo;
    private final TenantLogoStore logoStore;

    /** Vista de la marca para el panel. */
    public record Marca(String colorPrimario, String colorSecundario, String plantilla,
                        String fuente, String logoUrl) {}

    @Transactional(readOnly = true)
    public Marca get() {
        TenantJpaEntity t = tenant();
        return toMarca(t);
    }

    @Transactional
    public Marca update(String colorPrimario, String colorSecundario, String plantilla, String fuente) {
        TenantJpaEntity t = tenant();
        if (colorPrimario != null && !colorPrimario.isBlank()) {
            t.setColorPrimario(colorPrimario.trim());
        }
        // El secundario es opcional y se puede limpiar: blank/null → null (el front cae al primario).
        t.setColorSecundario(colorSecundario == null || colorSecundario.isBlank() ? null : colorSecundario.trim());
        // Plantilla: si viene, se normaliza a A/B/C (validada por @Pattern en el request); si no, se deja.
        if (plantilla != null && !plantilla.isBlank()) {
            t.setPlantilla(plantilla.trim().toUpperCase());
        }
        // fuente puede venir vacía para "sin preferencia": se guarda tal cual (null/blank permitido).
        t.setFuente(fuente == null || fuente.isBlank() ? null : fuente.trim());
        t.setUpdatedAt(Instant.now());
        tenantRepo.save(t);
        return toMarca(t);
    }

    @Transactional
    public Marca uploadLogo(byte[] bytes, String contentType) {
        TenantJpaEntity t = tenant();
        logoStore.save(t.getId(), bytes, contentType);
        return toMarca(t);
    }

    @Transactional
    public Marca clearLogo() {
        TenantJpaEntity t = tenant();
        logoStore.delete(t.getId());
        return toMarca(t);
    }

    private TenantJpaEntity tenant() {
        return tenantRepo.findById(tenantProvider.requireTenantId()).orElseThrow();
    }

    private Marca toMarca(TenantJpaEntity t) {
        String logoUrl = logoStore.exists(t.getId())
                ? LOGO_ENDPOINT + "?tenant=" + t.getSlug()
                : t.getLogoUrl();
        return new Marca(t.getColorPrimario(), t.getColorSecundario(), t.getPlantilla(), t.getFuente(), logoUrl);
    }
}
