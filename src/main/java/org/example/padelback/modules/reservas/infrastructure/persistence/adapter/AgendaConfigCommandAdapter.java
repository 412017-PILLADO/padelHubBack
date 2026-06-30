package org.example.padelback.modules.reservas.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import org.example.padelback.domain.port.TenantProvider;
import org.example.padelback.modules.reservas.domain.exception.BloqueoNoEncontradoException;
import org.example.padelback.modules.reservas.domain.exception.CanchaInvalidaException;
import org.example.padelback.modules.reservas.domain.exception.CanchaNoEncontradaException;
import org.example.padelback.modules.reservas.domain.exception.ComplejoNoResueltoException;
import org.example.padelback.modules.reservas.domain.exception.DuracionInvalidaException;
import org.example.padelback.modules.reservas.domain.model.CanchaEstado;
import org.example.padelback.modules.reservas.domain.model.ComplejoEstado;
import org.example.padelback.modules.reservas.domain.model.TipoPared;
import org.example.padelback.modules.reservas.domain.model.config.AgendaConfig;
import org.example.padelback.modules.reservas.domain.port.AgendaConfigCommandPort;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.BloqueoJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.CanchaJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.ComplejoJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.HorarioComplejoJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.BloqueoJpaRepository;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.CanchaJpaRepository;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.ComplejoJpaRepository;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.HorarioComplejoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class AgendaConfigCommandAdapter implements AgendaConfigCommandPort {

    private final TenantProvider tenantProvider;
    private final ComplejoJpaRepository complejoRepo;
    private final CanchaJpaRepository canchaRepo;
    private final HorarioComplejoJpaRepository horarioRepo;
    private final BloqueoJpaRepository bloqueoRepo;

    @Override
    @Transactional
    public void guardarHorarios(boolean breakOn, LocalTime breakFrom, LocalTime breakTo,
                                List<AgendaConfig.DiaConfig> week) {
        Long tenantId = tenantProvider.requireTenantId();
        ComplejoJpaEntity complejo = resolverComplejo(tenantId);

        horarioRepo.deleteByTenantIdAndComplejoId(tenantId, complejo.getId());
        horarioRepo.flush();

        for (AgendaConfig.DiaConfig d : week) {
            if (!d.open()) {
                continue;
            }
            LocalTime from = d.from();
            LocalTime to = d.to();
            if (from == null || to == null || !from.isBefore(to)) {
                throw new IllegalArgumentException("Franja inválida para el día " + d.diaSemana() + ": from < to requerido");
            }
            boolean conBreak = breakOn && breakFrom != null && breakTo != null
                    && breakFrom.isBefore(breakTo)
                    && breakFrom.isAfter(from) && breakTo.isBefore(to);
            if (conBreak) {
                insertarFranja(complejo.getId(), d.diaSemana(), from, breakFrom);
                insertarFranja(complejo.getId(), d.diaSemana(), breakTo, to);
            } else {
                insertarFranja(complejo.getId(), d.diaSemana(), from, to);
            }
        }
    }

    private void insertarFranja(Long complejoId, int diaSemana, LocalTime inicio, LocalTime fin) {
        // tenant_id lo estampa TenantEntityListener desde el TenantContext.
        HorarioComplejoJpaEntity h = HorarioComplejoJpaEntity.builder()
                .complejoId(complejoId)
                .diaSemana(diaSemana)
                .horaInicio(inicio)
                .horaFin(fin)
                .build();
        horarioRepo.save(h);
    }

    @Override
    @Transactional
    public void actualizarDuraciones(int pasoMinutos, List<Integer> duraciones, int duracionDefault) {
        if (pasoMinutos < 5 || pasoMinutos > 120) {
            throw new DuracionInvalidaException("pasoMinutos debe estar entre 5 y 120");
        }
        if (duraciones == null || duraciones.isEmpty()) {
            throw new DuracionInvalidaException("Se requiere al menos una duración permitida");
        }
        if (duraciones.stream().anyMatch(v -> v == null || v <= 0)) {
            throw new DuracionInvalidaException("Las duraciones deben ser positivas");
        }
        if (!duraciones.contains(duracionDefault)) {
            throw new DuracionInvalidaException(
                    "La duración default (" + duracionDefault + ") debe estar entre las permitidas " + duraciones);
        }
        Long tenantId = tenantProvider.requireTenantId();
        ComplejoJpaEntity complejo = resolverComplejo(tenantId);
        complejo.setPasoMinutos(pasoMinutos);
        complejo.setDuracionesPermitidas(duraciones.stream().map(String::valueOf).collect(Collectors.joining(",")));
        complejo.setDuracionDefault(duracionDefault);
        complejoRepo.save(complejo);
    }

    @Override
    @Transactional
    public void actualizarContacto(AgendaConfig.Contacto contacto) {
        Long tenantId = tenantProvider.requireTenantId();
        ComplejoJpaEntity complejo = resolverComplejo(tenantId);
        complejo.setDireccion(limpiar(contacto.direccion()));
        complejo.setTelefono(limpiar(contacto.telefono()));
        complejo.setWhatsapp(limpiar(contacto.whatsapp()));
        complejo.setMapaUrl(limpiar(contacto.mapaUrl()));
        complejo.setInstagram(limpiarInstagram(contacto.instagram()));
        complejoRepo.save(complejo);
    }

    /** Trim; vacío → null (para no guardar cadenas en blanco que la landing mostraría vacías). */
    private static String limpiar(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    /** Igual que limpiar, pero saca un '@' inicial (el handle se guarda sin @, como en el seed). */
    private static String limpiarInstagram(String v) {
        String t = limpiar(v);
        if (t == null) {
            return null;
        }
        return t.startsWith("@") ? limpiar(t.substring(1)) : t;
    }

    @Override
    @Transactional
    public AgendaConfig.BloqueoItem crearBloqueo(LocalDate fecha, Long canchaId, String motivo) {
        Long tenantId = tenantProvider.requireTenantId();
        ComplejoJpaEntity complejo = resolverComplejo(tenantId);

        String canchaNombre = "Todo el complejo";
        if (canchaId != null) {
            CanchaJpaEntity cancha = canchaRepo.findByTenantIdAndIdAndActiveTrue(tenantId, canchaId)
                    .orElseThrow(() -> new ComplejoNoResueltoException("Cancha " + canchaId + " no encontrada"));
            canchaNombre = cancha.getNombre();
        }

        BloqueoJpaEntity bloqueo = BloqueoJpaEntity.builder()
                .complejoId(complejo.getId())
                .canchaId(canchaId)
                .fechaHoraDesde(fecha.atStartOfDay())
                .fechaHoraHasta(fecha.plusDays(1).atStartOfDay())
                .motivo(motivo)
                .build();
        BloqueoJpaEntity saved = bloqueoRepo.save(bloqueo);
        return new AgendaConfig.BloqueoItem(saved.getId(), fecha, canchaId, canchaNombre, motivo);
    }

    @Override
    @Transactional
    public void eliminarBloqueo(Long id) {
        Long tenantId = tenantProvider.requireTenantId();
        BloqueoJpaEntity bloqueo = bloqueoRepo.findByTenantIdAndIdAndActiveTrue(tenantId, id)
                .orElseThrow(() -> new BloqueoNoEncontradoException(id));
        bloqueo.markDeleted("panel");
        bloqueoRepo.save(bloqueo);
    }

    @Override
    @Transactional
    public AgendaConfig.CanchaItem crearCancha(Long complejoId, String nombre, Integer orden, boolean techada,
                                               String tipoPared, BigDecimal precioHora, String color) {
        Long tenantId = tenantProvider.requireTenantId();
        ComplejoJpaEntity complejo = resolverComplejoDestino(tenantId, complejoId);

        String nombreLimpio = limpiar(nombre);
        if (nombreLimpio == null) {
            throw new CanchaInvalidaException("El nombre de la cancha es obligatorio");
        }
        TipoPared pared = parsearTipoPared(tipoPared);
        validarPrecio(precioHora);
        int ordenFinal = orden != null ? orden : siguienteOrden(tenantId, complejo.getId());

        // tenant_id lo estampa TenantEntityListener desde el TenantContext.
        CanchaJpaEntity cancha = CanchaJpaEntity.builder()
                .complejoId(complejo.getId())
                .nombre(nombreLimpio)
                .orden(ordenFinal)
                .techada(techada)
                .tipoPared(pared)
                .precioHora(precioHora)
                .color(limpiar(color))
                .estado(CanchaEstado.ACTIVO)
                .build();
        CanchaJpaEntity saved = canchaRepo.save(cancha);
        return toItem(saved);
    }

    @Override
    @Transactional
    public AgendaConfig.CanchaItem actualizarCancha(Long id, String nombre, Integer orden, boolean techada,
                                                    String tipoPared, BigDecimal precioHora, String color,
                                                    String estado) {
        Long tenantId = tenantProvider.requireTenantId();
        CanchaJpaEntity cancha = canchaRepo.findByTenantIdAndIdAndActiveTrue(tenantId, id)
                .orElseThrow(() -> new CanchaNoEncontradaException(id));

        String nombreLimpio = limpiar(nombre);
        if (nombreLimpio == null) {
            throw new CanchaInvalidaException("El nombre de la cancha es obligatorio");
        }
        validarPrecio(precioHora);
        cancha.setNombre(nombreLimpio);
        if (orden != null) {
            cancha.setOrden(orden);
        }
        cancha.setTechada(techada);
        cancha.setTipoPared(parsearTipoPared(tipoPared));
        cancha.setPrecioHora(precioHora);
        cancha.setColor(limpiar(color));
        cancha.setEstado(parsearEstado(estado));
        CanchaJpaEntity saved = canchaRepo.save(cancha);
        return toItem(saved);
    }

    @Override
    @Transactional
    public void eliminarCancha(Long id) {
        Long tenantId = tenantProvider.requireTenantId();
        CanchaJpaEntity cancha = canchaRepo.findByTenantIdAndIdAndActiveTrue(tenantId, id)
                .orElseThrow(() -> new CanchaNoEncontradaException(id));
        // Soft-delete: la cancha desaparece de la disponibilidad nueva; las reservas existentes quedan.
        cancha.markDeleted("panel");
        canchaRepo.save(cancha);
    }

    /** El complejo destino: el indicado (validado del tenant) o el único activo si no se especifica. */
    private ComplejoJpaEntity resolverComplejoDestino(Long tenantId, Long complejoId) {
        if (complejoId == null) {
            return resolverComplejo(tenantId);
        }
        return complejoRepo.findByTenantIdAndIdAndActiveTrue(tenantId, complejoId)
                .filter(c -> c.getEstado() == ComplejoEstado.ACTIVO)
                .orElseThrow(() -> new ComplejoNoResueltoException("Complejo " + complejoId + " no encontrado"));
    }

    private int siguienteOrden(Long tenantId, Long complejoId) {
        return canchaRepo
                .findByTenantIdAndComplejoIdAndEstadoAndActiveTrueOrderByOrdenAsc(tenantId, complejoId, CanchaEstado.ACTIVO)
                .stream().mapToInt(CanchaJpaEntity::getOrden).max().orElse(0) + 1;
    }

    private static TipoPared parsearTipoPared(String valor) {
        String t = limpiar(valor);
        if (t == null) {
            throw new CanchaInvalidaException("El tipo de pared es obligatorio (CRISTAL, MURO o MIXTA)");
        }
        try {
            return TipoPared.valueOf(t.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new CanchaInvalidaException("Tipo de pared inválido: " + valor + " (CRISTAL, MURO o MIXTA)");
        }
    }

    private static CanchaEstado parsearEstado(String valor) {
        String t = limpiar(valor);
        if (t == null) {
            return CanchaEstado.ACTIVO;
        }
        try {
            return CanchaEstado.valueOf(t.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new CanchaInvalidaException("Estado inválido: " + valor + " (ACTIVO o INACTIVO)");
        }
    }

    private static void validarPrecio(BigDecimal precioHora) {
        if (precioHora != null && precioHora.signum() < 0) {
            throw new CanchaInvalidaException("El precio por hora no puede ser negativo");
        }
    }

    private static AgendaConfig.CanchaItem toItem(CanchaJpaEntity c) {
        return new AgendaConfig.CanchaItem(c.getId(), c.getNombre(), c.getOrden(), c.isTechada(),
                c.getTipoPared().name(), c.getPrecioHora(), c.getColor(), c.getEstado().name());
    }

    private ComplejoJpaEntity resolverComplejo(Long tenantId) {
        List<ComplejoJpaEntity> complejos =
                complejoRepo.findByTenantIdAndEstadoAndActiveTrue(tenantId, ComplejoEstado.ACTIVO);
        if (complejos.size() != 1) {
            throw new ComplejoNoResueltoException("No hay exactamente un complejo activo configurable");
        }
        return complejos.get(0);
    }
}
