package org.example.padelback.modules.reservas.presentation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.example.padelback.modules.reservas.application.ConsultarDisponibilidadUseCase;
import org.example.padelback.modules.reservas.domain.model.disponibilidad.SlotDisponibilidad;
import org.example.padelback.modules.reservas.presentation.dto.CanchaLibreResponse;
import org.example.padelback.modules.reservas.presentation.dto.SlotResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/disponibilidad")
@RequiredArgsConstructor
public class DisponibilidadController {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final ConsultarDisponibilidadUseCase useCase;

    @GetMapping
    public List<SlotResponse> disponibilidad(
            @RequestParam(required = false) Long complejoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Integer duracion) {
        return useCase.ejecutar(complejoId, fecha, duracion).stream()
                .map(this::toResponse)
                .toList();
    }

    private SlotResponse toResponse(SlotDisponibilidad slot) {
        List<CanchaLibreResponse> libres = slot.canchasLibres().stream()
                .map(c -> new CanchaLibreResponse(c.id(), c.nombre(), c.color(), c.techada(),
                        c.tipoPared().name(), c.precioHora()))
                .toList();
        return new SlotResponse(slot.hora().format(HORA), slot.disponible(), libres);
    }
}
