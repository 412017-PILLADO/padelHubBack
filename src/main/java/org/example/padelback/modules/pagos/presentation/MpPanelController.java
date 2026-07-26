package org.example.padelback.modules.pagos.presentation;

import java.time.Clock;
import java.util.Map;

import org.example.padelback.modules.pagos.application.ConectarMpUseCase;
import org.example.padelback.modules.pagos.presentation.dto.ConectarMpRequest;
import org.example.padelback.modules.pagos.presentation.dto.MpEstadoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Conexión de la cuenta de Mercado Pago del club, desde la config del panel. */
@RestController
@RequestMapping("/api/v1/pagos/mp")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class MpPanelController {

    private final ConectarMpUseCase conectarMp;
    private final Clock clock;

    @GetMapping("/estado")
    public MpEstadoResponse estado() {
        return MpEstadoResponse.from(conectarMp.estado(), clock.instant());
    }

    @PostMapping("/conectar")
    public Map<String, String> conectar(@Valid @RequestBody ConectarMpRequest req) {
        return Map.of("url", conectarMp.generarUrlAutorizacion(req.returnTo()));
    }

    @PostMapping("/desconectar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desconectar() {
        conectarMp.desconectar();
    }
}
