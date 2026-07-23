package org.example.padelback.modules.reservas.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.example.padelback.domain.port.TenantProvider;
import org.example.padelback.modules.reservas.domain.exception.BloqueoNoEncontradoException;
import org.example.padelback.modules.reservas.domain.exception.CanchaInvalidaException;
import org.example.padelback.modules.reservas.domain.exception.CanchaNoEncontradaException;
import org.example.padelback.modules.reservas.domain.exception.ComplejoNoResueltoException;
import org.example.padelback.modules.reservas.domain.exception.DuracionInvalidaException;
import org.example.padelback.modules.reservas.domain.model.CanchaEstado;
import org.example.padelback.modules.reservas.domain.model.ComplejoEstado;
import org.example.padelback.modules.reservas.domain.model.PrecioModo;
import org.example.padelback.modules.reservas.domain.model.TipoPared;
import org.example.padelback.modules.reservas.domain.model.config.AgendaConfig;
import org.example.padelback.modules.reservas.domain.model.disponibilidad.Franja;
import org.example.padelback.modules.reservas.domain.port.AgendaConfigCommandPort;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.BloqueoJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.CanchaJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.ComplejoJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.HorarioComplejoJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.PrecioFranjaJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.entity.ReservaJpaEntity;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.BloqueoJpaRepository;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.CanchaJpaRepository;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.ComplejoJpaRepository;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.HorarioComplejoJpaRepository;
import org.example.padelback.modules.reservas.infrastructure.persistence.repository.PrecioFranjaJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class AgendaConfigCommandAdapter implements AgendaConfigCommandPort {

    // Mismo predicado "ocupa el slot" que ReservaJpaRepository#OCUPA (no se puede reusar la constante
    // porque ese repo está fuera de esta ownership; se replica acá para las queries ad-hoc por
    // EntityManager que necesitan A2: avisar reservas afectadas por un cambio de horarios/bloqueo).
    private static final String RESERVA_OCUPA = "r.active = true "
            + "and r.estado <> org.example.padelback.modules.reservas.domain.model.ReservaEstado.CANCELADO "
            + "and (r.expiraEn is null or r.expiraEn > :ahora) ";

    private final TenantProvider tenantProvider;
    private final ComplejoJpaRepository complejoRepo;
    private final CanchaJpaRepository canchaRepo;
    private final HorarioComplejoJpaRepository horarioRepo;
    private final PrecioFranjaJpaRepository precioFranjaRepo;
    private final BloqueoJpaRepository bloqueoRepo;
    private final Clock clock;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public List<AgendaConfig.ReservaAfectada> guardarHorarios(boolean breakOn, LocalTime breakFrom, LocalTime breakTo,
                                List<AgendaConfig.DiaConfig> week) {
        Long tenantId = tenantProvider.requireTenantId();
        ComplejoJpaEntity complejo = resolverComplejo(tenantId);

        horarioRepo.deleteByTenantIdAndComplejoId(tenantId, complejo.getId());
        horarioRepo.flush();

        // Franjas efectivamente guardadas por día de semana: se arman en la misma pasada que se
        // insertan, para después calcular qué reservas quedaron fuera (A2).
        Map<Integer, List<Franja>> franjasPorDia = new HashMap<>();

        for (AgendaConfig.DiaConfig d : week) {
            if (!d.open()) {
                continue;
            }
            LocalTime from = d.from();
            LocalTime to = d.to();
            if (from == null || to == null) {
                throw new IllegalArgumentException("Franja inválida para el día " + d.diaSemana() + ": from < to requerido");
            }
            List<Franja> franjasDelDia;
            try {
                franjasDelDia = franjasDelDia(from, to, breakOn, breakFrom, breakTo);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Franja inválida para el día " + d.diaSemana() + ": from < to requerido");
            }
            for (Franja f : franjasDelDia) {
                insertarFranja(complejo.getId(), d.diaSemana(), f.inicio(), f.fin());
            }
            franjasPorDia.put(d.diaSemana(), franjasDelDia);
        }

        return reservasAfectadasPorHorarios(tenantId, complejo.getId(), franjasPorDia);
    }

    /**
     * Traduce el horario de UN día (apertura {@code from}-{@code to} + break común del complejo) a las
     * franjas efectivas a persistir. Función pura (sin persistencia) para poder testearla directo.
     *
     * <p>A1: la intersección del break con la franja de este día es REAL — efectivo =
     * {@code [max(breakFrom, from), min(breakTo, to)]} — y si es no vacía, la franja se parte en
     * {@code [from, breakFromEfectivo)} + {@code [breakToEfectivo, to)}, descartando el lado que quede
     * vacío. Antes se exigía que el break cayera COMPLETO dentro de la franja
     * ({@code breakFrom > from && breakTo < to}), así que un break que arrancaba justo en la apertura o
     * terminaba justo en el cierre se descartaba ENTERO y se vendían turnos durante el descanso.
     *
     * <p>M-medianoche: {@code "00:00"} en {@code to} o en {@code breakTo} se interpreta como cierre a
     * medianoche (24:00, el cierre cae al terminar el día), nunca como una apertura a las 00:00.
     *
     * @throws IllegalArgumentException si {@code from}/{@code to} no describen una franja válida (from &lt; to)
     */
    static List<Franja> franjasDelDia(LocalTime from, LocalTime to, boolean breakOn,
                                      LocalTime breakFrom, LocalTime breakTo) {
        int fromMin = minutosApertura(from);
        int toMin = minutosCierre(to);
        if (fromMin >= toMin) {
            throw new IllegalArgumentException("Franja inválida: from < to requerido");
        }

        // OJO: la validez del break se compara en minutos (no con LocalTime#isBefore crudo), porque
        // un breakTo = "00:00" (medianoche = 24:00) compararía como el MENOR horario contra cualquier
        // breakFrom y el break se descartaría siempre como "inválido".
        if (breakOn && breakFrom != null && breakTo != null) {
            int breakFromMin = minutosApertura(breakFrom);
            int breakToMin = minutosCierre(breakTo);
            if (breakFromMin >= breakToMin) {
                return List.of(new Franja(from, to));
            }
            int efDesde = Math.max(fromMin, breakFromMin);
            int efHasta = Math.min(toMin, breakToMin);
            if (efDesde < efHasta) {
                // Partimos la franja, descartando el lado que quede vacío (ej. break que arranca en la
                // apertura → solo queda la tarde; break que termina en el cierre → solo queda la mañana).
                List<Franja> franjas = new ArrayList<>();
                if (fromMin < efDesde) {
                    franjas.add(new Franja(from, horaDesdeMinutos(efDesde)));
                }
                if (efHasta < toMin) {
                    franjas.add(new Franja(horaDesdeMinutos(efHasta), to));
                }
                return franjas;
            }
        }
        return List.of(new Franja(from, to));
    }

    /** Minutos desde las 00:00 de una hora "de apertura" (00:00 = 0, apertura real a medianoche). */
    private static int minutosApertura(LocalTime t) {
        return t.toSecondOfDay() / 60;
    }

    /**
     * Minutos desde las 00:00 de una hora "de cierre": {@code 00:00} se interpreta como medianoche
     * (24:00, el cierre cae al terminar el día), nunca como la apertura del mismo día.
     */
    private static int minutosCierre(LocalTime t) {
        return t.equals(LocalTime.MIDNIGHT) ? 24 * 60 : t.toSecondOfDay() / 60;
    }

    /** Inversa de {@link #minutosCierre}/{@link #minutosApertura}: 1440 vuelve a ser {@code 00:00}. */
    private static LocalTime horaDesdeMinutos(int minutos) {
        return minutos >= 24 * 60 ? LocalTime.MIDNIGHT : LocalTime.ofSecondOfDay(minutos * 60L);
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

    /**
     * A2: reservas CONFIRMADO/PENDIENTE-vigente y futuras que, con las franjas recién guardadas,
     * quedan total o parcialmente fuera de la franja de su día de semana (incluido el día que pasó a
     * cerrado, con lista vacía). El cambio ya se guardó igual: esto es solo la advertencia para que el
     * panel avise al dueño, calculada en la misma transacción del guardado.
     */
    private List<AgendaConfig.ReservaAfectada> reservasAfectadasPorHorarios(Long tenantId, Long complejoId,
            Map<Integer, List<Franja>> franjasPorDia) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        List<ReservaJpaEntity> reservas = reservasActivasFuturas(tenantId, complejoId, ahora);
        if (reservas.isEmpty()) {
            return List.of();
        }
        Map<Long, String> nombresCancha = nombresCanchaPorId(tenantId, complejoId);

        List<AgendaConfig.ReservaAfectada> afectadas = new ArrayList<>();
        for (ReservaJpaEntity r : reservas) {
            LocalDate fecha = r.getInicio().toLocalDate();
            int diaSemana = fecha.getDayOfWeek().getValue() - 1; // LUNES=0 ... DOMINGO=6
            List<Franja> franjasDelDia = franjasPorDia.getOrDefault(diaSemana, List.of());
            boolean cubierta = franjasDelDia.stream().anyMatch(f -> cubreCompleta(f, fecha, r.getInicio(), r.getFin()));
            if (!cubierta) {
                afectadas.add(new AgendaConfig.ReservaAfectada(r.getId(), fecha, r.getInicio().toLocalTime(),
                        nombresCancha.getOrDefault(r.getCanchaId(), "—"), r.getClienteNombre()));
            }
        }
        return afectadas;
    }

    /** La franja "cubre" la reserva si [inicio, fin) entra entero en ella (cierre "00:00" = medianoche). */
    private static boolean cubreCompleta(Franja f, LocalDate fecha, LocalDateTime inicio, LocalDateTime fin) {
        LocalDateTime desdeFranja = fecha.atTime(f.inicio());
        LocalDateTime hastaFranja = f.fin().equals(LocalTime.MIDNIGHT)
                ? fecha.plusDays(1).atStartOfDay()
                : fecha.atTime(f.fin());
        return !inicio.isBefore(desdeFranja) && !fin.isAfter(hastaFranja);
    }

    private List<ReservaJpaEntity> reservasActivasFuturas(Long tenantId, Long complejoId, LocalDateTime ahora) {
        String jpql = "select r from ReservaJpaEntity r where r.tenantId = :tenantId and r.complejoId = :complejoId "
                + "and " + RESERVA_OCUPA + "and r.inicio > :ahora order by r.inicio asc";
        return entityManager.createQuery(jpql, ReservaJpaEntity.class)
                .setParameter("tenantId", tenantId)
                .setParameter("complejoId", complejoId)
                .setParameter("ahora", ahora)
                .getResultList();
    }

    @Override
    @Transactional
    public void actualizarDuraciones(int pasoMinutos, List<Integer> duraciones, int duracionDefault,
                                     boolean permitirOtrasDuraciones) {
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
                    "El turno principal (" + duracionDefault + ") debe estar entre las duraciones " + duraciones);
        }
        Long tenantId = tenantProvider.requireTenantId();
        ComplejoJpaEntity complejo = resolverComplejo(tenantId);
        complejo.setPasoMinutos(pasoMinutos);
        complejo.setDuracionesPermitidas(duraciones.stream().map(String::valueOf).collect(Collectors.joining(",")));
        complejo.setDuracionDefault(duracionDefault);
        complejo.setPermitirOtrasDuraciones(permitirOtrasDuraciones);
        complejoRepo.save(complejo);
    }

    @Override
    @Transactional
    public void actualizarPrecios(String precioModo, BigDecimal precioHoraGeneral) {
        PrecioModo modo = parsearPrecioModo(precioModo);
        validarPrecio(precioHoraGeneral);
        if (modo == PrecioModo.GENERAL && precioHoraGeneral == null) {
            throw new CanchaInvalidaException("Con precio general hay que cargar el precio por hora");
        }
        Long tenantId = tenantProvider.requireTenantId();
        ComplejoJpaEntity complejo = resolverComplejo(tenantId);
        if (modo == PrecioModo.GENERAL) {
            complejo.setPrecioHoraGeneral(precioHoraGeneral);
        } else if (precioHoraGeneral != null) {
            // POR_CANCHA con un precio general mandado explícitamente: se guarda igual (no se ignora
            // el dato que vino en el request).
            complejo.setPrecioHoraGeneral(precioHoraGeneral);
        }
        // M3: POR_CANCHA con precioHoraGeneral null → se PRESERVA el precio general ya almacenado (no
        // se pisa con null). Así GENERAL → POR_CANCHA → GENERAL no pierde el precio general al volver.
        complejo.setPrecioModo(modo);
        complejoRepo.save(complejo);
    }

    private static PrecioModo parsearPrecioModo(String valor) {
        String t = limpiar(valor);
        if (t == null) {
            throw new CanchaInvalidaException("El modo de precio es obligatorio (GENERAL o POR_CANCHA)");
        }
        try {
            return PrecioModo.valueOf(t.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new CanchaInvalidaException("Modo de precio inválido: " + valor + " (GENERAL o POR_CANCHA)");
        }
    }

    @Override
    @Transactional
    public void guardarPrecioFranjas(List<AgendaConfig.PrecioFranjaItem> franjas) {
        List<AgendaConfig.PrecioFranjaItem> lista = franjas == null ? List.of() : franjas;
        validarPrecioFranjas(lista);

        Long tenantId = tenantProvider.requireTenantId();
        ComplejoJpaEntity complejo = resolverComplejo(tenantId);

        // Replace-all, mismo patrón que guardarHorarios: borra todo e inserta de nuevo (lista vacía =
        // sin franjas de precio especial).
        precioFranjaRepo.deleteByTenantIdAndComplejoId(tenantId, complejo.getId());
        precioFranjaRepo.flush();

        for (AgendaConfig.PrecioFranjaItem f : lista) {
            // tenant_id lo estampa TenantEntityListener desde el TenantContext.
            PrecioFranjaJpaEntity e = PrecioFranjaJpaEntity.builder()
                    .complejoId(complejo.getId())
                    .horaDesde(f.desde())
                    .horaHasta(f.hasta())
                    .ajustePorcentaje(f.ajustePorcentaje())
                    .build();
            precioFranjaRepo.save(e);
        }
    }

    /**
     * Valida las franjas de precio a guardar (función pura, testeada aparte): ajuste porcentual
     * distinto de 0 en (-100, 300] (a -100 el turno saldría gratis; el tope evita typos absurdos),
     * {@code desde < hasta} (con {@code hasta = "00:00"} interpretado como medianoche/24:00, igual
     * criterio que {@link #franjasDelDia}) y sin solapes entre franjas (comparación en minutos).
     *
     * @throws CanchaInvalidaException con un mensaje claro si alguna franja es inválida
     */
    static void validarPrecioFranjas(List<AgendaConfig.PrecioFranjaItem> franjas) {
        for (AgendaConfig.PrecioFranjaItem f : franjas) {
            if (f.desde() == null || f.hasta() == null) {
                throw new CanchaInvalidaException("Franja de precio inválida: desde y hasta son obligatorios");
            }
            if (f.ajustePorcentaje() == null || f.ajustePorcentaje() == 0) {
                throw new CanchaInvalidaException("El ajuste de la franja no puede ser 0%");
            }
            if (f.ajustePorcentaje() <= -100 || f.ajustePorcentaje() > 300) {
                throw new CanchaInvalidaException("El ajuste de la franja debe estar entre -99% y +300%");
            }
            if (minutosApertura(f.desde()) >= minutosCierre(f.hasta())) {
                throw new CanchaInvalidaException("Franja de precio inválida: desde < hasta requerido");
            }
        }
        List<AgendaConfig.PrecioFranjaItem> ordenadas = franjas.stream()
                .sorted(Comparator.comparingInt(f -> minutosApertura(f.desde())))
                .toList();
        for (int i = 1; i < ordenadas.size(); i++) {
            int finAnterior = minutosCierre(ordenadas.get(i - 1).hasta());
            int inicioActual = minutosApertura(ordenadas.get(i).desde());
            if (inicioActual < finAnterior) {
                throw new CanchaInvalidaException("Las franjas de precio no pueden solaparse");
            }
        }
    }

    @Override
    @Transactional
    public void actualizarSena(boolean requiereSena, BigDecimal senaMonto, String senaAlias) {
        Long tenantId = tenantProvider.requireTenantId();
        ComplejoJpaEntity complejo = resolverComplejo(tenantId);
        if (!requiereSena) {
            // M3: apagar la seña solo cambia el flag; se PRESERVAN el monto y el alias ya cargados
            // (no se borran), así prender/apagar de nuevo no obliga a recargarlos.
            complejo.setRequiereSena(false);
            complejoRepo.save(complejo);
            return;
        }
        String alias = senaAlias == null ? null : senaAlias.trim();
        // Si el módulo está prendido necesitamos monto > 0 y un alias: sin eso el cliente no sabe
        // cuánto ni a dónde transferir la seña.
        if (senaMonto == null || senaMonto.signum() <= 0) {
            throw new CanchaInvalidaException("Si pedís seña, cargá un monto mayor a 0.");
        }
        if (alias == null || alias.isEmpty()) {
            throw new CanchaInvalidaException("Si pedís seña, cargá el alias donde reciben la transferencia.");
        }
        validarPrecio(senaMonto);
        complejo.setRequiereSena(true);
        complejo.setSenaMonto(senaMonto);
        complejo.setSenaAlias(alias);
        complejoRepo.save(complejo);
    }

    @Override
    @Transactional
    public void actualizarAutoasignacion(boolean autoasignacion) {
        Long tenantId = tenantProvider.requireTenantId();
        ComplejoJpaEntity complejo = resolverComplejo(tenantId);
        complejo.setAutoasignacion(autoasignacion);
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
    public List<AgendaConfig.ReservaAfectada> crearBloqueo(LocalDate fecha, Long canchaId, String motivo) {
        Long tenantId = tenantProvider.requireTenantId();
        ComplejoJpaEntity complejo = resolverComplejo(tenantId);

        if (canchaId != null) {
            canchaRepo.findByTenantIdAndIdAndActiveTrue(tenantId, canchaId)
                    .orElseThrow(() -> new ComplejoNoResueltoException("Cancha " + canchaId + " no encontrada"));
        }

        LocalDateTime desde = fecha.atStartOfDay();
        LocalDateTime hasta = fecha.plusDays(1).atStartOfDay();
        BloqueoJpaEntity bloqueo = BloqueoJpaEntity.builder()
                .complejoId(complejo.getId())
                .canchaId(canchaId)
                .fechaHoraDesde(desde)
                .fechaHoraHasta(hasta)
                .motivo(motivo)
                .build();
        bloqueoRepo.save(bloqueo);

        return reservasAfectadasPorBloqueo(tenantId, complejo.getId(), canchaId, desde, hasta);
    }

    /**
     * A2: reservas activas y futuras del día bloqueado (y de esa cancha, si el bloqueo es por cancha)
     * que solapan con la ventana bloqueada. El bloqueo se guarda igual: esto es la advertencia,
     * calculada en la misma transacción de la creación.
     */
    private List<AgendaConfig.ReservaAfectada> reservasAfectadasPorBloqueo(Long tenantId, Long complejoId,
            Long canchaId, LocalDateTime desde, LocalDateTime hasta) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        StringBuilder jpql = new StringBuilder(
                "select r from ReservaJpaEntity r where r.tenantId = :tenantId and r.complejoId = :complejoId "
                        + "and " + RESERVA_OCUPA + "and r.inicio > :ahora and r.inicio < :hasta and r.fin > :desde");
        if (canchaId != null) {
            jpql.append(" and r.canchaId = :canchaId");
        }
        jpql.append(" order by r.inicio asc");

        var query = entityManager.createQuery(jpql.toString(), ReservaJpaEntity.class)
                .setParameter("tenantId", tenantId)
                .setParameter("complejoId", complejoId)
                .setParameter("ahora", ahora)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta);
        if (canchaId != null) {
            query.setParameter("canchaId", canchaId);
        }
        List<ReservaJpaEntity> reservas = query.getResultList();
        if (reservas.isEmpty()) {
            return List.of();
        }
        Map<Long, String> nombresCancha = nombresCanchaPorId(tenantId, complejoId);
        return reservas.stream()
                .map(r -> new AgendaConfig.ReservaAfectada(r.getId(), r.getInicio().toLocalDate(),
                        r.getInicio().toLocalTime(), nombresCancha.getOrDefault(r.getCanchaId(), "—"), r.getClienteNombre()))
                .toList();
    }

    /** Nombre de TODAS las canchas no eliminadas del complejo (activas e inactivas), para armar avisos. */
    private Map<Long, String> nombresCanchaPorId(Long tenantId, Long complejoId) {
        return canchaRepo.findByTenantIdAndActiveTrue(tenantId).stream()
                .filter(c -> c.getComplejoId().equals(complejoId))
                .collect(Collectors.toMap(CanchaJpaEntity::getId, CanchaJpaEntity::getNombre));
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
        // M1: se respeta el estado que manda el panel (ACTIVO/INACTIVO); null se interpreta como
        // ACTIVO. No se fuerza ACTIVO acá: una cancha puede pasar a INACTIVO y volver sin perder datos.
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

    /**
     * M1: el próximo orden considera TODAS las canchas no eliminadas del complejo (ACTIVO e
     * INACTIVO), no solo las ACTIVO — si solo mirara ACTIVO, una cancha pasada a INACTIVO liberaría su
     * número de orden y una nueva cancha podría terminar duplicándolo.
     */
    private int siguienteOrden(Long tenantId, Long complejoId) {
        return canchaRepo.findByTenantIdAndActiveTrue(tenantId).stream()
                .filter(c -> c.getComplejoId().equals(complejoId))
                .mapToInt(CanchaJpaEntity::getOrden).max().orElse(0) + 1;
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
