package org.example.padelback.modules.reservas.presentation.dto;

import java.time.LocalTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

public record GuardarHorariosRequest(
        boolean breakOn,
        @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime breakFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime breakTo,
        @NotNull List<@Valid DiaRequest> week) {

    public record DiaRequest(
            int diaSemana,
            boolean open,
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime from,
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime to) {}
}
