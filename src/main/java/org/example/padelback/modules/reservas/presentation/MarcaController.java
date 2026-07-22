package org.example.padelback.modules.reservas.presentation;

import java.io.IOException;
import java.util.Set;

import org.example.padelback.modules.reservas.application.TenantBrandingService;
import org.example.padelback.modules.reservas.application.TenantBrandingService.Marca;
import org.example.padelback.modules.reservas.presentation.dto.MarcaRequest;
import org.example.padelback.modules.reservas.presentation.dto.MarcaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Marca del tenant (panel del OWNER): color primario, fuente y logo. El logo se guarda como bytes
 * en la base ({@code tenant_logos}) y se sirve por {@code /public/tenant/logo}.
 */
@RestController
@RequestMapping("/api/v1/agenda/marca")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class MarcaController {

    /** Formatos aceptados para el logo. */
    private static final Set<String> TIPOS_OK = Set.of("image/png", "image/jpeg", "image/webp", "image/svg+xml");
    /** Tope de tamaño del logo (512 KB): un logo de header no necesita más. */
    private static final long MAX_BYTES = 512 * 1024;

    private final TenantBrandingService branding;

    @GetMapping
    public MarcaResponse get() {
        return toResponse(branding.get());
    }

    @PutMapping
    public MarcaResponse update(@Valid @RequestBody MarcaRequest req) {
        return toResponse(branding.update(req.colorPrimario(), req.colorSecundario(), req.plantilla(), req.fuente()));
    }

    @PostMapping("/logo")
    public MarcaResponse subirLogo(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subí un archivo de imagen.");
        }
        String tipo = file.getContentType();
        if (tipo == null || !TIPOS_OK.contains(tipo.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Formato no soportado. Usá PNG, JPG, WEBP o SVG.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El logo supera los 512 KB. Subí una versión más liviana.");
        }
        try {
            return toResponse(branding.uploadLogo(file.getBytes(), tipo.toLowerCase()));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No pudimos leer el archivo.");
        }
    }

    @DeleteMapping("/logo")
    public MarcaResponse quitarLogo() {
        return toResponse(branding.clearLogo());
    }

    private static MarcaResponse toResponse(Marca m) {
        return new MarcaResponse(m.colorPrimario(), m.colorSecundario(), m.plantilla(), m.fuente(), m.logoUrl());
    }
}
