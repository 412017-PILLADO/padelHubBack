package org.example.padelback.modules.reservas.presentation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.example.padelback.modules.reservas.application.CancelarTurnoUseCase;
import org.example.padelback.modules.reservas.application.ConsultarDisponibilidadUseCase;
import org.example.padelback.modules.reservas.application.CrearReservaUseCase;
import org.example.padelback.modules.reservas.application.ListarPendientesDeSenaUseCase;
import org.example.padelback.modules.reservas.application.ListarTurnosDelDiaUseCase;
import org.example.padelback.modules.reservas.application.ValidarSenaUseCase;
import org.example.padelback.modules.reservas.domain.model.reserva.ReservaCreada;
import org.example.padelback.modules.reservas.domain.model.turno.PendienteDeSena;
import org.example.padelback.modules.reservas.domain.model.turno.TurnoDelDia;
import org.example.padelback.modules.reservas.presentation.dto.CanchaLibreResponse;
import org.example.padelback.modules.reservas.presentation.dto.CrearReservaManualRequest;
import org.example.padelback.modules.reservas.presentation.dto.PendienteResponse;
import org.example.padelback.modules.reservas.presentation.dto.ReservaResponse;
import org.example.padelback.modules.reservas.presentation.dto.SlotResponse;
import org.example.padelback.modules.reservas.presentation.dto.TurnoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/turnos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class TurnosController {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ListarTurnosDelDiaUseCase listar;
    private final CancelarTurnoUseCase cancelar;
    private final ListarPendientesDeSenaUseCase listarPendientes;
    private final ValidarSenaUseCase validarSena;
    private final CrearReservaUseCase crearReserva;
    private final ConsultarDisponibilidadUseCase disponibilidadUseCase;

    @GetMapping
    public List<TurnoResponse> turnos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return listar.ejecutar(fecha).stream().map(this::toResponse).toList();
    }

    /**
     * Disponibilidad para el form de reserva manual. Espejo de {@code GET /public/disponibilidad}
     * pero autenticado: el panel corre en el host raíz (sin subdominio), así que el tenant sale del
     * JWT y no del hostname.
     */
    @GetMapping("/disponibilidad")
    public List<SlotResponse> disponibilidad(
            @RequestParam(required = false) Long complejoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Integer duracion) {
        return disponibilidadUseCase.ejecutar(complejoId, fecha, duracion).stream()
                .map(s -> new SlotResponse(s.hora().format(HORA), s.disponible(),
                        s.canchasLibres().stream()
                                .map(c -> new CanchaLibreResponse(c.id(), c.nombre(), c.color(),
                                        c.techada(), c.tipoPared().name(), c.precioHora()))
                                .toList()))
                .toList();
    }

    /** Reserva manual del dueño: nace CONFIRMADA (p. ej. una seña que se transfirió fuera de término). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse crearManual(@Valid @RequestBody CrearReservaManualRequest req) {
        ReservaCreada creada = crearReserva.ejecutarManual(
                req.complejoId(), req.canchaId(), req.fecha(), req.hora(), req.duracion(),
                req.clienteNombre(), req.clienteWhatsapp());
        return new ReservaResponse(
                creada.id(), creada.canchaId(), creada.canchaNombre(),
                creada.inicio(), creada.fin(), creada.duracionMinutos(), creada.estado());
    }

    @PostMapping("/{id}/cancelar")
    public Map<String, Object> cancelar(@PathVariable Long id) {
        cancelar.ejecutar(id);
        return Map.of("id", id, "estado", "CANCELADO");
    }

    // --- Señas: validación manual desde el panel ---

    @GetMapping("/pendientes")
    public List<PendienteResponse> pendientes() {
        return listarPendientes.ejecutar().stream().map(this::toPendiente).toList();
    }

    @PostMapping("/{id}/confirmar-sena")
    public Map<String, Object> confirmarSena(@PathVariable Long id) {
        validarSena.confirmar(id);
        return Map.of("id", id, "estado", "CONFIRMADO");
    }

    @PostMapping("/{id}/rechazar-sena")
    public Map<String, Object> rechazarSena(@PathVariable Long id) {
        validarSena.rechazar(id);
        return Map.of("id", id, "estado", "CANCELADO");
    }

    private TurnoResponse toResponse(TurnoDelDia t) {
        return new TurnoResponse(t.id(), t.inicio().format(HORA), t.fin().format(HORA), t.clienteNombre(),
                t.clienteWhatsapp(), t.canchaNombre(), t.duracionMinutos(), t.estado());
    }

    private PendienteResponse toPendiente(PendienteDeSena p) {
        return new PendienteResponse(p.id(), p.inicio().format(FECHA), p.inicio().format(HORA),
                p.fin().format(HORA), p.clienteNombre(), p.clienteWhatsapp(), p.canchaNombre(),
                p.duracionMinutos(), p.expiraEn() != null ? p.expiraEn().format(ISO) : null);
    }
}
