package org.example.padelback.modules.pagos.presentation;

import java.net.URI;

import org.example.padelback.modules.pagos.application.ConectarMpUseCase;
import org.example.padelback.modules.pagos.application.CrearLinkSenaUseCase;
import org.example.padelback.modules.pagos.presentation.dto.CrearLinkSenaRequest;
import org.example.padelback.modules.pagos.presentation.dto.LinkSenaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Superficie pública del módulo de pagos. Llega tráfico SIN JWT y (salvo la preferencia) sin
 * X-Tenant: el callback OAuth trae el tenant firmado en el state y el webhook lo trae por slug.
 */
@RestController
@RequestMapping("/public/pagos/mp")
@RequiredArgsConstructor
public class MpPublicController {

    private final ConectarMpUseCase conectarMp;
    private final CrearLinkSenaUseCase crearLinkSena;

    /** Redirect de vuelta de MP tras autorizar. Siempre termina 302 al panel (ok o error). */
    @GetMapping("/oauth/callback")
    public ResponseEntity<Void> callback(@RequestParam(required = false) String code,
                                         @RequestParam String state) {
        String returnTo = conectarMp.procesarCallback(code, state);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(returnTo + "?mp=conectado"))
                .build();
    }

    /** Link de pago de la seña de una reserva recién creada (idempotente). Header X-Tenant. */
    @PostMapping("/preferencia")
    public LinkSenaResponse preferencia(@Valid @RequestBody CrearLinkSenaRequest req) {
        return new LinkSenaResponse(crearLinkSena.ejecutar(req.reservaId(), req.backUrl()));
    }
}
