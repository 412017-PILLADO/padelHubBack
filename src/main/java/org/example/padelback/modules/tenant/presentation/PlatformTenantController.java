package org.example.padelback.modules.tenant.presentation;

import java.util.List;

import org.example.padelback.modules.tenant.application.TenantAdminService;
import org.example.padelback.modules.tenant.application.TenantAdminService.TenantResumen;
import org.example.padelback.modules.tenant.application.TenantProvisioningService;
import org.example.padelback.modules.tenant.application.TenantProvisioningService.NuevoTenant;
import org.example.padelback.modules.tenant.application.TenantProvisioningService.Resultado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestión de tenants a nivel plataforma (panel de dev del super-admin): alta, listado, edición y
 * activación/baja de clubes. Protegido por seguridad ({@code /platform/**} requiere rol SUPERADMIN,
 * satisfecho por el JWT de super-admin o por {@code X-Platform-Key} vía filtro para scripts).
 */
@RestController
@RequestMapping("/platform/tenants")
@RequiredArgsConstructor
public class PlatformTenantController {

    private final TenantProvisioningService provisioning;
    private final TenantAdminService admin;

    public record ProvisionRequest(
            @NotBlank String slug,
            @NotBlank String name,
            String colorPrimario,
            String colorSecundario,
            String plantilla,
            String fuente,
            @NotBlank String ownerEmail,
            @NotBlank String ownerPassword,
            String direccion,
            String whatsapp,
            List<String> hosts) {}

    public record ProvisionResponse(long tenantId, String slug, long complejoId) {}

    public record UpdateRequest(String name, String colorPrimario, String colorSecundario,
                                String plantilla, String status) {}

    public record OwnerPasswordRequest(
            @NotBlank @Size(min = 8, message = "La password debe tener al menos 8 caracteres.") String password) {}

    @GetMapping
    public List<TenantResumen> listar() {
        return admin.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProvisionResponse crear(@Valid @RequestBody ProvisionRequest req) {
        Resultado r = provisioning.provision(new NuevoTenant(
                req.slug(), req.name(), req.colorPrimario(), req.colorSecundario(), req.plantilla(),
                req.fuente(), req.ownerEmail(), req.ownerPassword(), req.direccion(), req.whatsapp(),
                req.hosts()));
        return new ProvisionResponse(r.tenantId(), r.slug(), r.complejoId());
    }

    @PutMapping("/{id}")
    public TenantResumen actualizar(@PathVariable long id, @RequestBody UpdateRequest req) {
        return admin.actualizar(id, req.name(), req.colorPrimario(), req.colorSecundario(),
                req.plantilla(), req.status());
    }

    @PostMapping("/{id}/activar")
    public TenantResumen activar(@PathVariable long id) {
        return admin.cambiarEstado(id, true);
    }

    @PostMapping("/{id}/desactivar")
    public TenantResumen desactivar(@PathVariable long id) {
        return admin.cambiarEstado(id, false);
    }

    /** Resetea la password del owner del club (recuperación de acceso desde el panel). */
    @PutMapping("/{id}/owner-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetearPasswordOwner(@PathVariable long id, @Valid @RequestBody OwnerPasswordRequest req) {
        admin.resetearPasswordOwner(id, req.password());
    }

    /** Baja definitiva del club (borra todos sus datos). Irreversible; el panel pide confirmación. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable long id) {
        admin.eliminar(id);
    }
}
