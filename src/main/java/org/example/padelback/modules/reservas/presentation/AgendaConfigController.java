package org.example.padelback.modules.reservas.presentation;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.example.padelback.modules.reservas.application.ActualizarAutoasignacionUseCase;
import org.example.padelback.modules.reservas.application.ActualizarContactoUseCase;
import org.example.padelback.modules.reservas.application.ActualizarDuracionesUseCase;
import org.example.padelback.modules.reservas.application.ActualizarPreciosUseCase;
import org.example.padelback.modules.reservas.application.ActualizarSenaUseCase;
import org.example.padelback.modules.reservas.application.CargarAgendaConfigUseCase;
import org.example.padelback.modules.reservas.application.GestionBloqueosUseCase;
import org.example.padelback.modules.reservas.application.GestionCanchasUseCase;
import org.example.padelback.modules.reservas.application.GuardarHorariosUseCase;
import org.example.padelback.modules.reservas.domain.model.config.AgendaConfig;
import org.example.padelback.modules.reservas.presentation.dto.ActualizarAutoasignacionRequest;
import org.example.padelback.modules.reservas.presentation.dto.ActualizarCanchaRequest;
import org.example.padelback.modules.reservas.presentation.dto.ActualizarContactoRequest;
import org.example.padelback.modules.reservas.presentation.dto.ActualizarDuracionesRequest;
import org.example.padelback.modules.reservas.presentation.dto.ActualizarPreciosRequest;
import org.example.padelback.modules.reservas.presentation.dto.ActualizarSenaRequest;
import org.example.padelback.modules.reservas.presentation.dto.AgendaConfigResponse;
import org.example.padelback.modules.reservas.presentation.dto.CrearBloqueoRequest;
import org.example.padelback.modules.reservas.presentation.dto.CrearCanchaRequest;
import org.example.padelback.modules.reservas.presentation.dto.GuardarHorariosRequest;
import org.example.padelback.modules.reservas.presentation.dto.ReservasAfectadasResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agenda")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class AgendaConfigController {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final CargarAgendaConfigUseCase cargar;
    private final GuardarHorariosUseCase guardarHorarios;
    private final ActualizarDuracionesUseCase actualizarDuraciones;
    private final ActualizarPreciosUseCase actualizarPrecios;
    private final ActualizarSenaUseCase actualizarSena;
    private final ActualizarAutoasignacionUseCase actualizarAutoasignacion;
    private final ActualizarContactoUseCase actualizarContacto;
    private final GestionBloqueosUseCase gestionBloqueos;
    private final GestionCanchasUseCase gestionCanchas;

    @GetMapping("/config")
    public AgendaConfigResponse config() {
        return AgendaConfigResponse.from(cargar.ejecutar());
    }

    @PutMapping("/horarios")
    public ReservasAfectadasResponse horarios(@Valid @RequestBody GuardarHorariosRequest req) {
        List<AgendaConfig.DiaConfig> week = new ArrayList<>();
        for (GuardarHorariosRequest.DiaRequest d : req.week()) {
            week.add(new AgendaConfig.DiaConfig(d.diaSemana(), d.open(), d.from(), d.to()));
        }
        List<AgendaConfig.ReservaAfectada> afectadas =
                guardarHorarios.ejecutar(req.breakOn(), req.breakFrom(), req.breakTo(), week);
        return toReservasAfectadasResponse(afectadas);
    }

    @PutMapping("/duraciones")
    public AgendaConfigResponse duraciones(@Valid @RequestBody ActualizarDuracionesRequest req) {
        actualizarDuraciones.ejecutar(req.pasoMinutos(), req.duraciones(), req.duracionDefault(),
                req.permitirOtrasDuraciones());
        return AgendaConfigResponse.from(cargar.ejecutar());
    }

    @PutMapping("/precios")
    public AgendaConfigResponse precios(@Valid @RequestBody ActualizarPreciosRequest req) {
        actualizarPrecios.ejecutar(req.precioModo(), req.precioHoraGeneral());
        return AgendaConfigResponse.from(cargar.ejecutar());
    }

    @PutMapping("/sena")
    public AgendaConfigResponse sena(@Valid @RequestBody ActualizarSenaRequest req) {
        actualizarSena.ejecutar(req.requiereSena(), req.senaMonto(), req.senaAlias());
        return AgendaConfigResponse.from(cargar.ejecutar());
    }

    @PutMapping("/autoasignacion")
    public AgendaConfigResponse autoasignacion(@Valid @RequestBody ActualizarAutoasignacionRequest req) {
        actualizarAutoasignacion.ejecutar(req.autoasignacion());
        return AgendaConfigResponse.from(cargar.ejecutar());
    }

    @PutMapping("/contacto")
    public AgendaConfigResponse contacto(@Valid @RequestBody ActualizarContactoRequest req) {
        actualizarContacto.ejecutar(new AgendaConfig.Contacto(
                req.direccion(), req.telefono(), req.whatsapp(), req.mapaUrl(), req.instagram()));
        return AgendaConfigResponse.from(cargar.ejecutar());
    }

    @PostMapping("/bloqueos")
    public ReservasAfectadasResponse crearBloqueo(@Valid @RequestBody CrearBloqueoRequest req) {
        List<AgendaConfig.ReservaAfectada> afectadas = gestionBloqueos.crear(req.fecha(), req.canchaId(), req.motivo());
        return toReservasAfectadasResponse(afectadas);
    }

    @DeleteMapping("/bloqueos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarBloqueo(@PathVariable Long id) {
        gestionBloqueos.eliminar(id);
    }

    @PostMapping("/canchas")
    @ResponseStatus(HttpStatus.CREATED)
    public AgendaConfigResponse.CanchaResponse crearCancha(@Valid @RequestBody CrearCanchaRequest req) {
        AgendaConfig.CanchaItem item = gestionCanchas.crear(
                req.complejoId(), req.nombre(), req.orden(), req.techada(),
                req.tipoPared(), req.precioHora(), req.color());
        return toResponse(item);
    }

    @PutMapping("/canchas/{id}")
    public AgendaConfigResponse.CanchaResponse actualizarCancha(@PathVariable Long id,
                                                               @Valid @RequestBody ActualizarCanchaRequest req) {
        AgendaConfig.CanchaItem item = gestionCanchas.actualizar(
                id, req.nombre(), req.orden(), req.techada(),
                req.tipoPared(), req.precioHora(), req.color(), req.estado());
        return toResponse(item);
    }

    @DeleteMapping("/canchas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarCancha(@PathVariable Long id) {
        gestionCanchas.eliminar(id);
    }

    private static AgendaConfigResponse.CanchaResponse toResponse(AgendaConfig.CanchaItem c) {
        return new AgendaConfigResponse.CanchaResponse(c.id(), c.nombre(), c.orden(), c.techada(),
                c.tipoPared(), c.precioHora(), c.color(), c.estado());
    }

    private static ReservasAfectadasResponse toReservasAfectadasResponse(List<AgendaConfig.ReservaAfectada> afectadas) {
        List<ReservasAfectadasResponse.ReservaAfectadaResponse> items = afectadas.stream()
                .map(r -> new ReservasAfectadasResponse.ReservaAfectadaResponse(
                        r.id(), r.fecha().format(FECHA), r.hora().format(HORA), r.cancha(), r.cliente()))
                .toList();
        return new ReservasAfectadasResponse(items);
    }
}
