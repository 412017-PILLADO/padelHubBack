package org.example.padelback.modules.reservas.presentation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.example.padelback.modules.reservas.application.CancelarTurnoUseCase;
import org.example.padelback.modules.reservas.application.ListarTurnosDelDiaUseCase;
import org.example.padelback.modules.reservas.domain.model.turno.TurnoDelDia;
import org.example.padelback.modules.reservas.presentation.dto.TurnoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/turnos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class TurnosController {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final ListarTurnosDelDiaUseCase listar;
    private final CancelarTurnoUseCase cancelar;

    @GetMapping
    public List<TurnoResponse> turnos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return listar.ejecutar(fecha).stream().map(this::toResponse).toList();
    }

    @PostMapping("/{id}/cancelar")
    public Map<String, Object> cancelar(@PathVariable Long id) {
        cancelar.ejecutar(id);
        return Map.of("id", id, "estado", "CANCELADO");
    }

    private TurnoResponse toResponse(TurnoDelDia t) {
        return new TurnoResponse(t.id(), t.inicio().format(HORA), t.fin().format(HORA), t.clienteNombre(),
                t.clienteWhatsapp(), t.canchaNombre(), t.duracionMinutos(), t.estado());
    }
}
