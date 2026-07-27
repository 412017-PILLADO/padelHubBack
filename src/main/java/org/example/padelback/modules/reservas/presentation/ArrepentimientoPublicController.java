package org.example.padelback.modules.reservas.presentation;

import java.util.Map;

import org.example.padelback.modules.reservas.application.RegistrarArrepentimientoUseCase;
import org.example.padelback.modules.reservas.presentation.dto.RegistrarArrepentimientoRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Botón de arrepentimiento (Res. 424/2020): público, sin registro, código inmediato. */
@RestController
@RequestMapping("/public/arrepentimiento")
@RequiredArgsConstructor
public class ArrepentimientoPublicController {

    private final RegistrarArrepentimientoUseCase registrar;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> crear(@Valid @RequestBody RegistrarArrepentimientoRequest req) {
        return Map.of("codigo", registrar.ejecutar(
                req.nombre(), req.whatsapp(), req.detalle(), req.reservaFecha(), req.empresa()));
    }
}
