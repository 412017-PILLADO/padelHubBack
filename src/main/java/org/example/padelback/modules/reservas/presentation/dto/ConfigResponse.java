package org.example.padelback.modules.reservas.presentation.dto;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.example.padelback.modules.reservas.domain.model.config.ConfigPublico;

public record ConfigResponse(
        Tenant tenant,
        Complejo complejo,
        int pasoMinutos,
        List<Integer> duracionesPermitidas,
        int duracionDefault,
        List<Cancha> canchas,
        List<Horario> horarios) {

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    public record Tenant(String nombre, String colorPrimario, String fuente,
                         boolean mostrarPrecios, boolean requiereTelefono) {}

    public record Complejo(Long id, String nombre, String direccion, String telefono,
                           String whatsapp, String mapaUrl, String instagram) {}

    public record Cancha(Long id, String nombre, int orden, boolean techada, String tipoPared,
                         BigDecimal precioHora, String color) {}

    public record Horario(int diaSemana, String horaInicio, String horaFin) {}

    public static ConfigResponse from(ConfigPublico c) {
        return new ConfigResponse(
                new Tenant(c.tenant().nombre(), c.tenant().colorPrimario(), c.tenant().fuente(),
                        c.tenant().mostrarPrecios(), c.tenant().requiereTelefono()),
                new Complejo(c.complejo().id(), c.complejo().nombre(), c.complejo().direccion(),
                        c.complejo().telefono(), c.complejo().whatsapp(), c.complejo().mapaUrl(),
                        c.complejo().instagram()),
                c.pasoMinutos(),
                c.duracionesPermitidas(),
                c.duracionDefault(),
                c.canchas().stream()
                        .map(ca -> new Cancha(ca.id(), ca.nombre(), ca.orden(), ca.techada(), ca.tipoPared(),
                                ca.precioHora(), ca.color())).toList(),
                c.horarios().stream()
                        .map(h -> new Horario(h.diaSemana(), h.horaInicio().format(HM), h.horaFin().format(HM)))
                        .toList());
    }
}
