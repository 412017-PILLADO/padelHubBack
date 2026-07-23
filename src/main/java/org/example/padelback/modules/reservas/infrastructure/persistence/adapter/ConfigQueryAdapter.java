package org.example.padelback.modules.reservas.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.example.padelback.domain.port.TenantProvider;
import org.example.padelback.modules.reservas.domain.model.CanchaEstado;
import org.example.padelback.modules.reservas.domain.model.ComplejoEstado;
import org.example.padelback.modules.reservas.domain.model.PrecioModo;
import org.example.padelback.modules.reservas.domain.model.config.ConfigPublico;
import org.example.padelback.modules.reservas.domain.model.config.ConfigPublico.CanchaInfo;
import org.example.padelback.modules.reservas.domain.model.config.ConfigPublico.ComplejoInfo;
import org.example.padelback.modules.reservas.domain.model.config.ConfigPublico.HorarioInfo;
import org.example.padelback.modules.reservas.domain.model.config.ConfigPublico.PrecioFranjaInfo;
import org.example.padelback.modules.reservas.domain.model.config.ConfigPublico.TenantInfo;
import org.example.padelback.modules.reservas.domain.port.ConfigQueryPort;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.CanchaJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.ComplejoJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.CanchaJpaRepository;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.ComplejoJpaRepository;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.HorarioComplejoJpaRepository;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.PrecioFranjaJpaRepository;
import org.example.padelback.modules.tenant.infrastructure.persistence.TenantLogoStore;
import org.example.padelback.modules.tenant.infrastructure.persistence.entity.TenantJpaEntity;
import org.example.padelback.modules.tenant.infrastructure.persistence.repository.TenantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class ConfigQueryAdapter implements ConfigQueryPort {

    /** Path público que sirve el logo guardado en la base (bytes). */
    private static final String LOGO_ENDPOINT = "/public/tenant/logo";

    private final TenantProvider tenantProvider;
    private final TenantJpaRepository tenantRepo;
    private final TenantLogoStore logoStore;
    private final ComplejoJpaRepository complejoRepo;
    private final CanchaJpaRepository canchaRepo;
    private final HorarioComplejoJpaRepository horarioRepo;
    private final PrecioFranjaJpaRepository precioFranjaRepo;

    @Override
    @Transactional(readOnly = true)
    public List<Long> complejosActivos() {
        Long tenantId = tenantProvider.requireTenantId();
        return complejoRepo.findByTenantIdAndEstadoAndActiveTrue(tenantId, ComplejoEstado.ACTIVO)
                .stream().map(ComplejoJpaEntity::getId).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConfigPublico> cargar(Long complejoId) {
        Long tenantId = tenantProvider.requireTenantId();

        ComplejoJpaEntity complejo = complejoRepo.findByTenantIdAndIdAndActiveTrue(tenantId, complejoId).orElse(null);
        if (complejo == null || complejo.getEstado() != ComplejoEstado.ACTIVO) {
            return Optional.empty();
        }
        TenantJpaEntity tenant = tenantRepo.findById(tenantId).orElseThrow();

        // M1: el config público SOLO muestra canchas ACTIVO (el panel del dueño, en cambio, lista
        // también las INACTIVO para poder reactivarlas) — la landing no debe ofrecer una cancha inactiva.
        List<CanchaInfo> canchas = canchaRepo
                .findByTenantIdAndComplejoIdAndEstadoAndActiveTrueOrderByOrdenAsc(tenantId, complejo.getId(), CanchaEstado.ACTIVO)
                .stream()
                .map(c -> new CanchaInfo(c.getId(), c.getNombre(), c.getOrden(), c.isTechada(),
                        c.getTipoPared().name(), precioEfectivo(complejo, c), c.getColor()))
                .toList();

        List<HorarioInfo> horarios = horarioRepo
                .findByTenantIdAndComplejoIdAndActiveTrue(tenantId, complejo.getId()).stream()
                .map(h -> new HorarioInfo(h.getDiaSemana(), h.getHoraInicio(), h.getHoraFin()))
                .sorted((a, b) -> a.diaSemana() != b.diaSemana()
                        ? Integer.compare(a.diaSemana(), b.diaSemana())
                        : a.horaInicio().compareTo(b.horaInicio()))
                .toList();

        // Si hay logo subido (bytes en base) se sirve por el endpoint público; si no, cae a la URL
        // externa que el tenant tenga configurada (o null → el front muestra solo el nombre).
        // El slug va como query param: el <img> no manda X-Tenant y en prod front/back están en
        // orígenes separados, así el endpoint resuelve el tenant sin depender del host.
        String logoUrl = logoStore.exists(tenantId)
                ? LOGO_ENDPOINT + "?tenant=" + tenant.getSlug()
                : tenant.getLogoUrl();
        TenantInfo tenantInfo = new TenantInfo(tenant.getName(), tenant.getColorPrimario(),
                tenant.getColorSecundario(), tenant.getFuente(), logoUrl, tenant.isMostrarPrecios(),
                tenant.isRequiereTelefono(), tenant.getPlantilla());
        ComplejoInfo complejoInfo = new ComplejoInfo(complejo.getId(), complejo.getNombre(), complejo.getDireccion(),
                complejo.getTelefono(), complejo.getWhatsapp(), complejo.getMapaUrl(), complejo.getInstagram());

        // Si no permite otras duraciones, la pública solo ofrece el turno principal.
        List<Integer> duraciones = complejo.isPermitirOtrasDuraciones()
                ? parseDuraciones(complejo.getDuracionesPermitidas())
                : List.of(complejo.getDuracionDefault());

        // Franjas de precio especial (generales del complejo), para que la landing muestre la promo
        // ("desde $X"); sin id porque acá es solo lectura pública.
        List<PrecioFranjaInfo> precioFranjas = precioFranjaRepo
                .findByTenantIdAndComplejoIdAndActiveTrue(tenantId, complejo.getId()).stream()
                .sorted((a, b) -> a.getHoraDesde().compareTo(b.getHoraDesde()))
                .map(f -> new PrecioFranjaInfo(f.getHoraDesde(), f.getHoraHasta(), f.getAjustePorcentaje()))
                .toList();

        return Optional.of(new ConfigPublico(tenantInfo, complejoInfo, complejo.getPasoMinutos(),
                duraciones, complejo.getDuracionDefault(), complejo.isPermitirOtrasDuraciones(),
                complejo.isRequiereSena(), complejo.getSenaMonto(), complejo.getSenaAlias(),
                complejo.isAutoasignacion(),
                canchas, horarios, precioFranjas));
    }

    /** Precio por hora aplicable a la cancha según el modo del complejo (GENERAL o POR_CANCHA). */
    private static BigDecimal precioEfectivo(ComplejoJpaEntity complejo, CanchaJpaEntity cancha) {
        return complejo.getPrecioModo() == PrecioModo.GENERAL
                ? complejo.getPrecioHoraGeneral()
                : cancha.getPrecioHora();
    }

    private static List<Integer> parseDuraciones(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> s.matches("\\d+"))
                .map(Integer::valueOf)
                .toList();
    }
}
