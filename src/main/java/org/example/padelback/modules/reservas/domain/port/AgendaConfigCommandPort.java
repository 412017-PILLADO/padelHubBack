package org.example.padelback.modules.reservas.domain.port;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.example.padelback.modules.reservas.domain.model.config.AgendaConfig;

public interface AgendaConfigCommandPort {
    void guardarHorarios(boolean breakOn, LocalTime breakFrom, LocalTime breakTo, List<AgendaConfig.DiaConfig> week);
    void actualizarDuraciones(int pasoMinutos, List<Integer> duraciones, int duracionDefault,
                              boolean permitirOtrasDuraciones);
    void actualizarPrecios(String precioModo, BigDecimal precioHoraGeneral);
    void actualizarSena(boolean requiereSena, BigDecimal senaMonto, String senaAlias);
    void actualizarAutoasignacion(boolean autoasignacion);
    void actualizarContacto(AgendaConfig.Contacto contacto);
    AgendaConfig.BloqueoItem crearBloqueo(LocalDate fecha, Long canchaId, String motivo);
    void eliminarBloqueo(Long id);

    AgendaConfig.CanchaItem crearCancha(Long complejoId, String nombre, Integer orden, boolean techada,
                                        String tipoPared, BigDecimal precioHora, String color);
    AgendaConfig.CanchaItem actualizarCancha(Long id, String nombre, Integer orden, boolean techada,
                                             String tipoPared, BigDecimal precioHora, String color, String estado);
    void eliminarCancha(Long id);
}
