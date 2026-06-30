package org.example.padelback.modules.reservas.presentation.dto;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.example.padelback.modules.reservas.domain.model.config.AgendaConfig;

public record AgendaConfigResponse(
        String nombreComplejo,
        int pasoMinutos,
        List<Integer> duraciones,
        int duracionDefault,
        boolean breakOn, String breakFrom, String breakTo,
        List<DiaResponse> week,
        List<BloqueoResponse> bloqueos,
        List<CanchaResponse> canchas,
        ContactoResponse contacto) {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public record DiaResponse(int diaSemana, boolean open, String from, String to) {}

    public record BloqueoResponse(Long id, String fecha, Long canchaId, String canchaNombre, String motivo) {}

    public record CanchaResponse(Long id, String nombre, int orden, boolean techada, String tipoPared,
                                 BigDecimal precioHora, String color, String estado) {}

    public record ContactoResponse(String direccion, String telefono, String whatsapp,
                                   String mapaUrl, String instagram) {}

    public static AgendaConfigResponse from(AgendaConfig config) {
        List<DiaResponse> week = config.week().stream()
                .map(d -> new DiaResponse(d.diaSemana(), d.open(), d.from().format(HORA), d.to().format(HORA)))
                .toList();
        List<BloqueoResponse> bloqueos = config.bloqueos().stream()
                .map(b -> new BloqueoResponse(b.id(), b.fecha().format(FECHA), b.canchaId(), b.canchaNombre(), b.motivo()))
                .toList();
        List<CanchaResponse> canchas = config.canchas().stream()
                .map(c -> new CanchaResponse(c.id(), c.nombre(), c.orden(), c.techada(), c.tipoPared(),
                        c.precioHora(), c.color(), c.estado()))
                .toList();
        AgendaConfig.Contacto c = config.contacto();
        ContactoResponse contacto = new ContactoResponse(
                c.direccion(), c.telefono(), c.whatsapp(), c.mapaUrl(), c.instagram());
        return new AgendaConfigResponse(
                config.nombreComplejo(),
                config.pasoMinutos(),
                config.duraciones(),
                config.duracionDefault(),
                config.breakOn(),
                config.breakFrom().format(HORA),
                config.breakTo().format(HORA),
                week,
                bloqueos,
                canchas,
                contacto);
    }
}
