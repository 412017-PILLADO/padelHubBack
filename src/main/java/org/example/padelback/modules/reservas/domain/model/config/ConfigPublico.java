package org.example.padelback.modules.reservas.domain.model.config;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/** Config pública que consume la landing de reserva: branding del tenant + complejo + canchas + horarios. */
public record ConfigPublico(
        TenantInfo tenant,
        ComplejoInfo complejo,
        int pasoMinutos,
        List<Integer> duracionesPermitidas,
        int duracionDefault,
        boolean permitirOtrasDuraciones,
        boolean requiereSena,
        BigDecimal senaMonto,
        String senaAlias,
        boolean autoasignacion,
        List<CanchaInfo> canchas,
        List<HorarioInfo> horarios) {

    public record TenantInfo(String nombre, String colorPrimario, String colorSecundario, String fuente,
                             String logoUrl, boolean mostrarPrecios, boolean requiereTelefono,
                             String plantilla) {}

    public record ComplejoInfo(Long id, String nombre, String direccion, String telefono,
                               String whatsapp, String mapaUrl, String instagram) {}

    public record CanchaInfo(Long id, String nombre, int orden, boolean techada, String tipoPared,
                             BigDecimal precioHora, String color) {}

    public record HorarioInfo(int diaSemana, LocalTime horaInicio, LocalTime horaFin) {}
}
