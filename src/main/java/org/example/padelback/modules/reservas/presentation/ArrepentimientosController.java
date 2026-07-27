package org.example.padelback.modules.reservas.presentation;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.example.padelback.modules.reservas.application.GestionArrepentimientosUseCase;
import org.example.padelback.modules.reservas.presentation.dto.ArrepentimientoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Gestión de solicitudes de arrepentimiento (Res. 424/2020) desde el panel del dueño. */
@RestController
@RequestMapping("/api/v1/arrepentimientos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class ArrepentimientosController {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final GestionArrepentimientosUseCase gestion;
    private final Clock clock;

    @GetMapping
    public List<ArrepentimientoResponse> listar() {
        return gestion.listar().stream()
                .map(a -> new ArrepentimientoResponse(a.getId(), a.getCodigo(), a.getNombre(),
                        a.getWhatsapp(), a.getDetalle(), a.getReservaFecha(), a.isGestionado(),
                        a.getCreatedAt() != null
                                ? FECHA_HORA.withZone(clock.getZone()).format(a.getCreatedAt())
                                : null))
                .toList();
    }

    @PostMapping("/{id}/gestionar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void gestionar(@PathVariable Long id) {
        gestion.marcarGestionado(id);
    }
}
