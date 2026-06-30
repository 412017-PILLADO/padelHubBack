package org.example.padelback.modules.reservas.presentation;

import org.example.padelback.modules.reservas.application.ConsultarConfigUseCase;
import org.example.padelback.modules.reservas.presentation.dto.ConfigResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConsultarConfigUseCase useCase;

    @GetMapping
    public ConfigResponse config(@RequestParam(required = false) Long complejoId) {
        return ConfigResponse.from(useCase.ejecutar(complejoId));
    }
}
