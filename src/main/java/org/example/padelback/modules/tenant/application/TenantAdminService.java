package org.example.padelback.modules.tenant.application;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.example.padelback.modules.tenant.domain.model.TenantStatus;
import org.example.padelback.modules.tenant.infrastructure.persistence.entity.TenantJpaEntity;
import org.example.padelback.modules.tenant.infrastructure.persistence.repository.TenantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Gestión de tenants desde el panel de plataforma (super-admin): listar, editar datos de marca y
 * activar/desactivar. El alta vive en {@link TenantProvisioningService}.
 */
@Service
@RequiredArgsConstructor
public class TenantAdminService {

    private final TenantJpaRepository tenantRepo;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    /** Tablas tenant-scoped a limpiar en la baja, en orden hijo→padre (respeta las FKs). */
    private static final List<String> CASCADE = List.of(
            "reservas", "bloqueos", "horarios_complejo", "canchas", "complejos",
            "usuarios", "tenant_dominios", "tenant_logos");

    /** Fila del listado de clubes. */
    public record TenantResumen(long id, String slug, String name, String status,
                                String colorPrimario, String colorSecundario, String logoUrl,
                                String plantilla) {}

    @Transactional(readOnly = true)
    public List<TenantResumen> listar() {
        return tenantRepo.findAll().stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .map(TenantAdminService::toResumen)
                .toList();
    }

    @Transactional
    public TenantResumen actualizar(long id, String name, String colorPrimario, String colorSecundario,
                                    String plantilla, String status) {
        TenantJpaEntity t = tenantRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Club no encontrado."));
        if (name != null && !name.isBlank()) {
            t.setName(name.trim());
        }
        if (colorPrimario != null && !colorPrimario.isBlank()) {
            t.setColorPrimario(colorPrimario.trim());
        }
        // Secundario opcional: se puede limpiar (null/blank → null).
        t.setColorSecundario(colorSecundario == null || colorSecundario.isBlank() ? null : colorSecundario.trim());
        if (plantilla != null && !plantilla.isBlank()) {
            t.setPlantilla(parsePlantilla(plantilla));
        }
        if (status != null && !status.isBlank()) {
            t.setStatus(parseStatus(status));
        }
        t.setUpdatedAt(Instant.now());
        tenantRepo.save(t);
        return toResumen(t);
    }

    /**
     * Baja DEFINITIVA de un club: borra todos sus datos tenant-scoped (reservas, canchas, complejos,
     * horarios, usuarios, dominios, logo) y el tenant en sí, en una transacción. Usa JdbcTemplate
     * (SQL directo) para no arrastrar el filtro de tenant ni el soft-delete: es una limpieza total.
     */
    @Transactional
    public void eliminar(long id) {
        TenantJpaEntity t = tenantRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Club no encontrado."));
        long tid = t.getId();
        for (String tabla : CASCADE) {
            jdbc.update("DELETE FROM " + tabla + " WHERE tenant_id = ?", tid);
        }
        jdbc.update("DELETE FROM tenants WHERE id = ?", tid);
    }

    @Transactional
    public TenantResumen cambiarEstado(long id, boolean activo) {
        TenantJpaEntity t = tenantRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Club no encontrado."));
        t.setStatus(activo ? TenantStatus.ACTIVE : TenantStatus.INACTIVE);
        t.setUpdatedAt(Instant.now());
        tenantRepo.save(t);
        return toResumen(t);
    }

    /**
     * Resetea la password del owner de un club desde el panel de plataforma (recuperación de acceso
     * cuando el club perdió su clave). 404 si el club no existe o si no tiene usuario OWNER. La
     * validación de longitud mínima ya la hace el DTO del controller ({@code @Size(min = 8)}).
     */
    @Transactional
    public void resetearPasswordOwner(long id, String nuevaPassword) {
        tenantRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Club no encontrado."));
        String hash = passwordEncoder.encode(nuevaPassword);
        // active = true: no reactivar/tocar un owner soft-eliminado (mismo criterio que el login,
        // findByTenantIdAndEmailAndActiveTrue).
        int actualizados = jdbc.update(
                "UPDATE usuarios SET password_hash = ?, updated_at = ? WHERE tenant_id = ? AND rol = 'OWNER' "
                        + "AND active = true",
                hash, Timestamp.from(Instant.now()), id);
        if (actualizados == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El club no tiene un usuario owner.");
        }
    }

    private static TenantStatus parseStatus(String s) {
        try {
            return TenantStatus.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Estado inválido (ACTIVE/INACTIVE).");
        }
    }

    private static String parsePlantilla(String s) {
        String up = s.trim().toUpperCase();
        if (!up.matches("[ABC]")) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Plantilla inválida (A/B/C).");
        }
        return up;
    }

    private static TenantResumen toResumen(TenantJpaEntity t) {
        return new TenantResumen(t.getId(), t.getSlug(), t.getName(), t.getStatus().name(),
                t.getColorPrimario(), t.getColorSecundario(), t.getLogoUrl(), t.getPlantilla());
    }
}
