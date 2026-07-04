package org.example.padelback.modules.reservas.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

import org.example.padelback.infrastructure.config.AntiAbuseProperties;
import org.example.padelback.modules.reservas.domain.exception.ComplejoNoResueltoException;
import org.example.padelback.modules.reservas.domain.exception.DuracionInvalidaException;
import org.example.padelback.modules.reservas.domain.exception.LimiteReservasPorIpException;
import org.example.padelback.modules.reservas.domain.exception.LimiteTurnosPorTelefonoException;
import org.example.padelback.modules.reservas.domain.exception.SlotNoDisponibleException;
import org.example.padelback.modules.reservas.domain.exception.SolicitudInvalidaException;
import org.example.padelback.modules.reservas.domain.exception.TelefonoRequeridoException;
import org.example.padelback.modules.reservas.domain.model.disponibilidad.AgendaDelDia;
import org.example.padelback.modules.reservas.domain.model.disponibilidad.CanchaLibre;
import org.example.padelback.modules.reservas.domain.model.ReservaEstado;
import org.example.padelback.modules.reservas.domain.model.disponibilidad.SlotDisponibilidad;
import org.example.padelback.modules.reservas.domain.model.reserva.NuevaReserva;
import org.example.padelback.modules.reservas.domain.model.reserva.ReservaCreada;
import org.example.padelback.modules.reservas.domain.port.AgendaQueryPort;
import org.example.padelback.modules.reservas.domain.port.ReservaCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CrearReservaUseCase {

    /** Ventana fija que tiene el cliente para pagar la seña antes de que la reserva PENDIENTE venza. */
    private static final int SENA_VENTANA_MINUTOS = 15;

    private final AgendaQueryPort agendaQueryPort;
    private final ReservaCommandPort reservaCommandPort;
    private final AvailabilityService availabilityService;
    private final Clock clock;
    private final AntiAbuseProperties props;

    /**
     * @param canchaId null = "cualquiera disponible" (el back asigna la menos cargada); si viene,
     *                 esa cancha debe estar libre en el slot.
     */
    @Transactional
    public ReservaCreada ejecutar(Long complejoId, Long canchaId, LocalDate fecha, LocalTime hora,
                                  Integer duracion, String clienteNombre, String clienteWhatsapp,
                                  String honeypot, String clientIp) {
        if (honeypot != null && !honeypot.isBlank()) {
            throw new SolicitudInvalidaException("Solicitud inválida");
        }
        Long complejoResuelto = resolverComplejo(complejoId);

        if (reservaCommandPort.tenantRequiereTelefono() && (clienteWhatsapp == null || clienteWhatsapp.isBlank())) {
            throw new TelefonoRequeridoException("El WhatsApp del cliente es requerido");
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        if (LocalDateTime.of(fecha, hora).isBefore(ahora)) {
            throw new SlotNoDisponibleException("El horario " + hora + " del " + fecha + " ya pasó");
        }

        if (clientIp != null && !clientIp.isBlank()) {
            Instant desde = clock.instant().minus(props.ip().ventana());
            if (reservaCommandPort.reservasDesdeIpDesde(clientIp, desde) >= props.ip().maxPorVentana()) {
                throw new LimiteReservasPorIpException(
                        "Hubo demasiadas reservas desde este dispositivo. Probá más tarde o escribinos.");
            }
        }

        String telefonoNorm = Telefonos.normalizar(clienteWhatsapp);
        if (!telefonoNorm.isEmpty()) {
            long activos = reservaCommandPort.telefonosDeReservasActivasFuturas(ahora).stream()
                    .map(Telefonos::normalizar)
                    .filter(telefonoNorm::equals)
                    .count();
            if (activos >= props.maxTurnosPorTelefono()) {
                throw new LimiteTurnosPorTelefonoException("Ya tenés " + props.maxTurnosPorTelefono()
                        + " turnos reservados con ese WhatsApp. Si necesitás otro o cambiar alguno, escribinos.");
            }
        }

        AgendaDelDia agenda = agendaQueryPort.cargarAgendaDelDia(complejoResuelto, fecha)
                .orElseThrow(() -> new SlotNoDisponibleException("Complejo no encontrado"));

        int duracionEfectiva = duracion != null ? duracion : agenda.duracionDefault();
        if (!agenda.duracionesPermitidas().isEmpty() && !agenda.duracionesPermitidas().contains(duracionEfectiva)) {
            throw new DuracionInvalidaException(
                    "Duración " + duracionEfectiva + " no permitida. Opciones: " + agenda.duracionesPermitidas());
        }

        List<SlotDisponibilidad> slots = availabilityService.calcular(agenda, duracionEfectiva, ahora);
        SlotDisponibilidad slot = slots.stream()
                .filter(s -> s.hora().equals(hora) && s.disponible())
                .findFirst()
                .orElseThrow(() -> new SlotNoDisponibleException("El horario " + hora + " no está disponible"));

        CanchaLibre elegida = elegirCancha(slot.canchasLibres(), canchaId, fecha);

        LocalDateTime inicio = LocalDateTime.of(fecha, hora);
        LocalDateTime fin = inicio.plusMinutes(duracionEfectiva);

        // Si el complejo pide seña, la reserva nace PENDIENTE y retiene la cancha hasta que venza la
        // ventana de pago; si no, se confirma directo. La validación de la seña la hace el panel.
        ReservaEstado estado = agenda.requiereSena() ? ReservaEstado.PENDIENTE : ReservaEstado.CONFIRMADO;
        LocalDateTime expiraEn = estado == ReservaEstado.PENDIENTE
                ? ahora.plusMinutes(SENA_VENTANA_MINUTOS)
                : null;

        NuevaReserva nueva = new NuevaReserva(
                complejoResuelto, elegida.id(), inicio, fin, duracionEfectiva,
                clienteNombre, clienteWhatsapp, clientIp, estado, expiraEn);

        return reservaCommandPort.crearSiLibre(nueva);
    }

    /**
     * Si el cliente eligió una cancha, esa debe estar entre las libres del slot. Si no eligió
     * ("cualquiera"), se asigna la menos cargada ese día (desempate por id ascendente).
     */
    private CanchaLibre elegirCancha(List<CanchaLibre> libres, Long canchaId, LocalDate fecha) {
        if (libres.isEmpty()) {
            throw new SlotNoDisponibleException("No hay canchas libres en ese horario");
        }
        if (canchaId != null) {
            return libres.stream()
                    .filter(c -> c.id().equals(canchaId))
                    .findFirst()
                    .orElseThrow(() -> new SlotNoDisponibleException("La cancha elegida no está libre en ese horario"));
        }
        return libres.stream()
                .min(Comparator
                        .comparingInt((CanchaLibre c) -> reservaCommandPort.reservasQueOcupanEseDia(c.id(), fecha))
                        .thenComparing(CanchaLibre::id))
                .orElseThrow();
    }

    private Long resolverComplejo(Long complejoId) {
        if (complejoId != null) {
            if (!agendaQueryPort.complejoActivoExiste(complejoId)) {
                throw new ComplejoNoResueltoException("Complejo " + complejoId + " no encontrado");
            }
            return complejoId;
        }
        List<Long> activos = agendaQueryPort.complejosActivos();
        if (activos.size() != 1) {
            throw new ComplejoNoResueltoException(
                    "Se requiere complejoId: el tenant tiene " + activos.size() + " complejos activos");
        }
        return activos.get(0);
    }
}
