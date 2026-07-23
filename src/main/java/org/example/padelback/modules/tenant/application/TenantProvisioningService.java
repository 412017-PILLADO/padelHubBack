package org.example.padelback.modules.tenant.application;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.regex.Pattern;

import org.example.padelback.modules.tenant.infrastructure.persistence.repository.TenantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Alta de un tenant nuevo en un solo paso (onboarding de un club): crea tenant + dominios + complejo
 * base + canchas + horarios + usuario OWNER, en una transacción. Reemplaza el "duplicar el seed a
 * mano". Usa JdbcTemplate para no arrastrar el listener de tenant ni el auditing en un alta puntual.
 */
@Service
@RequiredArgsConstructor
public class TenantProvisioningService {

    private static final String BY = "provision";
    private static final String DEFAULT_COLOR = "#2747ff";
    private static final String DEFAULT_FUENTE = "Hanken Grotesk";
    private static final String DEFAULT_PLANTILLA = "A";

    /** Hostname simple: letras/números/guiones por segmento, segmentos separados por punto. Sin
     * esquema ({@code http://}), sin espacios, sin barras. */
    private static final Pattern HOST_PATTERN =
            Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*(\\.[a-z0-9]+(-[a-z0-9]+)*)*$");

    private final JdbcTemplate jdbc;
    private final TenantJpaRepository tenantRepo;
    private final PasswordEncoder passwordEncoder;

    public record NuevoTenant(
            String slug, String name, String colorPrimario, String colorSecundario, String plantilla,
            String fuente, String ownerEmail, String ownerPassword,
            String direccion, String whatsapp, List<String> hosts) {}

    public record Resultado(long tenantId, String slug, long complejoId) {}

    @Transactional
    public Resultado provision(NuevoTenant in) {
        String slug = in.slug() == null ? "" : in.slug().trim().toLowerCase();
        if (slug.isBlank() || !slug.matches("[a-z0-9-]{2,80}")) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Slug inválido: usá minúsculas, números y guiones (2-80).");
        }
        if (in.name() == null || in.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Falta el nombre del club.");
        }
        if (in.ownerEmail() == null || in.ownerEmail().isBlank()
                || in.ownerPassword() == null || in.ownerPassword().length() < 8) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Owner inválido: email y password (≥8) requeridos.");
        }
        if (tenantRepo.findBySlug(slug).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un club con ese slug.");
        }

        // Hosts normalizados (minúsculas, sin blancos). Validamos formato y que estén libres ANTES de
        // crear nada: tenant_dominios.host es único, así que sin este chequeo un host repetido o con
        // formato inválido (ej. con esquema o espacios) daría un 500 crudo o un dato corrupto.
        List<String> hosts = in.hosts() == null ? List.of() : in.hosts().stream()
                .filter(h -> h != null && !h.isBlank())
                .map(h -> h.trim().toLowerCase())
                .toList();
        for (String host : hosts) {
            if (!HOST_PATTERN.matcher(host).matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Dominio inválido: '" + host + "' (usá letras, números, guiones y puntos, sin "
                                + "espacios ni esquema).");
            }
            Integer usados = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM tenant_dominios WHERE host = ?", Integer.class, host);
            if (usados != null && usados > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El dominio '" + host + "' ya está en uso por otro club.");
            }
        }

        Timestamp now = Timestamp.from(Instant.now());
        String color = blankTo(in.colorPrimario(), DEFAULT_COLOR);
        String colorSec = emptyToNull(in.colorSecundario()); // opcional; el front cae al primario si es null
        String plantilla = normalizePlantilla(in.plantilla());
        String fuente = blankTo(in.fuente(), DEFAULT_FUENTE);

        // Los checks de slug/host de arriba tienen ventana de carrera (TOCTOU): si dos altas pisan el
        // mismo slug/host casi al mismo tiempo, el UNIQUE de la tabla es la última defensa. Sin este
        // catch, esa carrera daría un 500 crudo en vez del mismo 409 del check previo.
        try {
            return provisionarSinRace(slug, hosts, now, color, colorSec, plantilla, fuente, in);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un club con ese slug o dominio.", e);
        }
    }

    private Resultado provisionarSinRace(String slug, List<String> hosts, Timestamp now, String color,
                                         String colorSec, String plantilla, String fuente, NuevoTenant in) {
        // 1) Tenant
        long tenantId = insertReturningId(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO tenants (slug, name, status, color_primario, color_secundario, plantilla, "
                            + "fuente, mostrar_precios, requiere_telefono, created_at, updated_at) "
                            + "VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, slug);
            ps.setString(2, in.name().trim());
            ps.setString(3, "ACTIVE");
            ps.setString(4, color);
            ps.setString(5, colorSec);
            ps.setString(6, plantilla);
            ps.setString(7, fuente);
            ps.setBoolean(8, true);
            ps.setBoolean(9, true);
            ps.setTimestamp(10, now);
            ps.setTimestamp(11, now);
            return ps;
        });

        // 2) Dominios (opcionales, ya normalizados y validados arriba). Sin ellos igual funciona por
        //    header X-Tenant (slug).
        for (String host : hosts) {
            jdbc.update("INSERT INTO tenant_dominios (tenant_id, host) VALUES (?, ?)", tenantId, host);
        }

        // 3) Complejo base (paso 30, duraciones 60/90/120, default 90)
        long complejoId = insertReturningId(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO complejos (tenant_id, nombre, direccion, whatsapp, paso_minutos, "
                            + "duraciones_permitidas, duracion_default, estado, created_at, updated_at, "
                            + "created_by, updated_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, tenantId);
            ps.setString(2, in.name().trim());
            ps.setString(3, emptyToNull(in.direccion()));
            ps.setString(4, emptyToNull(in.whatsapp()));
            ps.setInt(5, 30);
            ps.setString(6, "60,90,120");
            ps.setInt(7, 90);
            ps.setString(8, "ACTIVO");
            ps.setTimestamp(9, now);
            ps.setTimestamp(10, now);
            ps.setString(11, BY);
            ps.setString(12, BY);
            return ps;
        });

        // 4) Dos canchas por defecto (el club las edita después)
        for (int i = 1; i <= 2; i++) {
            jdbc.update("INSERT INTO canchas (tenant_id, complejo_id, nombre, orden, techada, tipo_pared, "
                            + "color, estado, created_at, updated_at, created_by, updated_by) "
                            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                    tenantId, complejoId, "Cancha " + i, i, false, "CRISTAL",
                    color, "ACTIVO", now, now, BY, BY);
        }

        // 5) Horarios: todos los días 08:00–23:00
        LocalTime apertura = LocalTime.of(8, 0);
        LocalTime cierre = LocalTime.of(23, 0);
        for (int dia = 0; dia < 7; dia++) {
            jdbc.update("INSERT INTO horarios_complejo (tenant_id, complejo_id, dia_semana, hora_inicio, "
                            + "hora_fin, created_at, updated_at, created_by, updated_by) VALUES (?,?,?,?,?,?,?,?,?)",
                    tenantId, complejoId, dia, Time(apertura), Time(cierre), now, now, BY, BY);
        }

        // 6) Usuario OWNER
        jdbc.update("INSERT INTO usuarios (tenant_id, email, password_hash, rol, estado, created_at, "
                        + "updated_at, created_by, updated_by) VALUES (?,?,?,?,?,?,?,?,?)",
                tenantId, in.ownerEmail().trim().toLowerCase(),
                passwordEncoder.encode(in.ownerPassword()), "OWNER", "ACTIVO", now, now, BY, BY);

        return new Resultado(tenantId, slug, complejoId);
    }

    private long insertReturningId(org.springframework.jdbc.core.PreparedStatementCreator psc) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(psc, kh);
        Number key = kh.getKey();
        if (key == null) {
            throw new IllegalStateException("No se generó la clave del insert");
        }
        return key.longValue();
    }

    private static java.sql.Time Time(LocalTime t) {
        return java.sql.Time.valueOf(t);
    }

    private static String blankTo(String v, String def) {
        return v == null || v.isBlank() ? def : v.trim();
    }

    private static String emptyToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    /** Normaliza la plantilla a A/B/C (default A ante null o valor desconocido). */
    private static String normalizePlantilla(String v) {
        if (v == null || v.isBlank()) {
            return DEFAULT_PLANTILLA;
        }
        String up = v.trim().toUpperCase();
        return up.matches("[ABC]") ? up : DEFAULT_PLANTILLA;
    }
}
