package org.example.padelback.modules.reservas.presentation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.example.padelback.modules.reservas.application.CrearReservaUseCase;
import org.example.padelback.modules.reservas.domain.model.reserva.ReservaCreada;
import org.example.padelback.modules.reservas.infrastructure.web.ClientIpResolver;
import org.example.padelback.modules.reservas.presentation.dto.CrearReservaRequest;
import org.example.padelback.modules.reservas.presentation.dto.ReservaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final CrearReservaUseCase crearReservaUseCase;
    private final ClientIpResolver clientIpResolver;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse crear(@Valid @RequestBody CrearReservaRequest req, HttpServletRequest http) {
        String ip = clientIpResolver.resolve(http);
        ReservaCreada creada = crearReservaUseCase.ejecutar(
                req.complejoId(), req.canchaId(), req.fecha(), req.hora(), req.duracion(),
                req.clienteNombre(), req.clienteWhatsapp(), req.empresa(), ip);
        return new ReservaResponse(
                creada.id(), creada.canchaId(), creada.canchaNombre(),
                creada.inicio(), creada.fin(), creada.duracionMinutos(), creada.estado());
    }
}
