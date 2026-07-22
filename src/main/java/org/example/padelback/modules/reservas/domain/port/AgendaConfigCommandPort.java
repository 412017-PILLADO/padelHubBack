package org.example.padelback.modules.reservas.domain.port;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.example.padelback.modules.reservas.domain.model.config.AgendaConfig;

public interface AgendaConfigCommandPort {
    /** @return reservas CONFIRMADO/PENDIENTE-vigente futuras que quedaron fuera de la nueva franja de su día. */
    List<AgendaConfig.ReservaAfectada> guardarHorarios(boolean breakOn, LocalTime breakFrom, LocalTime breakTo,
                                                       List<AgendaConfig.DiaConfig> week);
    void actualizarDuraciones(int pasoMinutos, List<Integer> duraciones, int duracionDefault,
                              boolean permitirOtrasDuraciones);
    void actualizarPrecios(String precioModo, BigDecimal precioHoraGeneral);
    /** Replace-all: reemplaza TODAS las franjas de precio del complejo (lista vacía = sin franjas). */
    void guardarPrecioFranjas(List<AgendaConfig.PrecioFranjaItem> franjas);
    void actualizarSena(boolean requiereSena, BigDecimal senaMonto, String senaAlias);
    void actualizarAutoasignacion(boolean autoasignacion);
    void actualizarContacto(AgendaConfig.Contacto contacto);
    /** @return reservas activas futuras (de esa cancha si el bloqueo es por cancha) que solapan con el bloqueo. */
    List<AgendaConfig.ReservaAfectada> crearBloqueo(LocalDate fecha, Long canchaId, String motivo);
    void eliminarBloqueo(Long id);

    AgendaConfig.CanchaItem crearCancha(Long complejoId, String nombre, Integer orden, boolean techada,
                                        String tipoPared, BigDecimal precioHora, String color);
    AgendaConfig.CanchaItem actualizarCancha(Long id, String nombre, Integer orden, boolean techada,
                                             String tipoPared, BigDecimal precioHora, String color, String estado);
    void eliminarCancha(Long id);
}
