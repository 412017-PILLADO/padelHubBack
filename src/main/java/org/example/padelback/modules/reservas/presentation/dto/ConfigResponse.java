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
        boolean permitirOtrasDuraciones,
        boolean requiereSena,
        BigDecimal senaMonto,
        String senaAlias,
        boolean autoasignacion,
        List<Cancha> canchas,
        List<Horario> horarios,
        List<PrecioFranja> precioFranjas) {

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    public record Tenant(String nombre, String colorPrimario, String colorSecundario, String fuente,
                         String logoUrl, boolean mostrarPrecios, boolean requiereTelefono,
                         String plantilla) {}

    public record Complejo(Long id, String nombre, String direccion, String telefono,
                           String whatsapp, String mapaUrl, String instagram) {}

    public record Cancha(Long id, String nombre, int orden, boolean techada, String tipoPared,
                         BigDecimal precioHora, String color) {}

    public record Horario(int diaSemana, String horaInicio, String horaFin) {}

    /** Franja horaria con precio especial GENERAL del complejo (sin id, promo pública "desde $X"). */
    public record PrecioFranja(String desde, String hasta, int ajustePorcentaje) {}

    public static ConfigResponse from(ConfigPublico c) {
        return new ConfigResponse(
                new Tenant(c.tenant().nombre(), c.tenant().colorPrimario(), c.tenant().colorSecundario(),
                        c.tenant().fuente(), c.tenant().logoUrl(), c.tenant().mostrarPrecios(),
                        c.tenant().requiereTelefono(), c.tenant().plantilla()),
                new Complejo(c.complejo().id(), c.complejo().nombre(), c.complejo().direccion(),
                        c.complejo().telefono(), c.complejo().whatsapp(), c.complejo().mapaUrl(),
                        c.complejo().instagram()),
                c.pasoMinutos(),
                c.duracionesPermitidas(),
                c.duracionDefault(),
                c.permitirOtrasDuraciones(),
                c.requiereSena(),
                c.senaMonto(),
                c.senaAlias(),
                c.autoasignacion(),
                c.canchas().stream()
                        .map(ca -> new Cancha(ca.id(), ca.nombre(), ca.orden(), ca.techada(), ca.tipoPared(),
                                ca.precioHora(), ca.color())).toList(),
                c.horarios().stream()
                        .map(h -> new Horario(h.diaSemana(), h.horaInicio().format(HM), h.horaFin().format(HM)))
                        .toList(),
                c.precioFranjas().stream()
                        .map(f -> new PrecioFranja(f.desde().format(HM), f.hasta().format(HM), f.ajustePorcentaje()))
                        .toList());
    }
}
