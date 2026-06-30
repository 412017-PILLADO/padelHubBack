package org.example.padelback.modules.reservas.domain.model.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Config de agenda del complejo tal como la edita el panel. Las franjas de apertura (filas de
 * {@code HorarioComplejo}) se traducen a un esquema semanal {@code week[7]} + un descanso opcional
 * (break) común; al guardar se hace la traducción inversa.
 */
public record AgendaConfig(
        String nombreComplejo,
        int pasoMinutos,
        List<Integer> duraciones,
        int duracionDefault,
        boolean breakOn, LocalTime breakFrom, LocalTime breakTo,
        List<DiaConfig> week,
        List<BloqueoItem> bloqueos,
        List<CanchaItem> canchas,
        Contacto contacto) {

    public record DiaConfig(int diaSemana, boolean open, LocalTime from, LocalTime to) {}

    /** Un bloqueo: {@code canchaId} null => todo el complejo (canchaNombre = "Todo el complejo"). */
    public record BloqueoItem(Long id, LocalDate fecha, Long canchaId, String canchaNombre, String motivo) {}

    public record CanchaItem(Long id, String nombre, int orden, boolean techada, String tipoPared,
                             BigDecimal precioHora, String color, String estado) {}

    /** Datos de contacto/ubicación del complejo que se muestran en la landing pública. */
    public record Contacto(String direccion, String telefono, String whatsapp, String mapaUrl, String instagram) {}
}
