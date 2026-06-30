package org.example.padelback.modules.reservas.application;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.example.padelback.modules.reservas.domain.model.disponibilidad.AgendaCancha;
import org.example.padelback.modules.reservas.domain.model.disponibilidad.AgendaDelDia;
import org.example.padelback.modules.reservas.domain.model.disponibilidad.CanchaLibre;
import org.example.padelback.modules.reservas.domain.model.disponibilidad.CanchaRef;
import org.example.padelback.modules.reservas.domain.model.disponibilidad.Franja;
import org.example.padelback.modules.reservas.domain.model.disponibilidad.Ocupacion;
import org.example.padelback.modules.reservas.domain.model.disponibilidad.SlotDisponibilidad;
import org.springframework.stereotype.Service;

/**
 * Cálculo de disponibilidad con turnos contiguos y duración variable (función pura). Los inicios se
 * <b>encadenan de a {@code duracion}</b> desde la apertura de cada franja: 08:00 → 08:00+dur →
 * 08:00+2·dur … Un turno termina justo cuando arranca el siguiente, así que no hay huecos muertos
 * dentro de una franja (modelo "uno sale, entra el siguiente"). No hay grilla de granularidad
 * independiente: el {@code pasoMinutos} del complejo ya no influye en la generación de inicios.
 *
 * <p>Una cancha está libre en el inicio {@code T} si {@code [T, T+duracion)} entra dentro de una
 * franja de apertura y no solapa ninguna de sus ocupaciones (reservas confirmadas + bloqueos). Un
 * slot es {@code disponible} si al menos una cancha está libre, y devuelve todas las libres para que
 * el cliente pueda elegir una o dejar que el back asigne ("cualquiera").
 *
 * <p>Como la grilla queda anclada a la apertura, el cierre puede dejar un resto menor a {@code
 * duracion} sin turno (ej. franja 08:00-23:00 con duración 120 da 7 turnos hasta las 22:00 y la
 * última hora queda libre): es inherente al modelo contiguo cuando el largo de la franja no es
 * múltiplo de la duración.
 */
@Service
public class AvailabilityService {

    /**
     * @param agenda  snapshot del día (franjas compartidas + canchas con sus ocupaciones)
     * @param duracion duración pedida en minutos (ya validada contra las permitidas)
     * @param ahora    hora de pared del negocio; los inicios ya pasados se devuelven no disponibles
     */
    public List<SlotDisponibilidad> calcular(AgendaDelDia agenda, int duracion, LocalDateTime ahora) {
        TreeSet<LocalTime> todasLasHoras = new TreeSet<>();
        // cancha -> inicios libres (orden de inserción para reflejar el orden de canchas)
        Map<CanchaRef, TreeSet<LocalTime>> libresPorCancha = new LinkedHashMap<>();

        for (AgendaCancha ac : agenda.canchas()) {
            TreeSet<LocalTime> libres = new TreeSet<>();
            for (Franja f : agenda.franjas()) {
                // Aritmética en minutos del día: evita el wrap de LocalTime.plusMinutes a medianoche
                // (con franja que cierra 23:00 y un inicio que cae justo ahí, T+duracion daría 00:00
                // y el bucle nunca terminaría). El paso del encadenado ES la duración: cada turno
                // arranca donde termina el anterior. El último inicio es el que entra entero en la franja.
                int inicioMin = f.inicio().toSecondOfDay() / 60;
                int finMin = f.fin().toSecondOfDay() / 60;
                for (int m = inicioMin; m + duracion <= finMin; m += duracion) {
                    LocalTime t = LocalTime.ofSecondOfDay(m * 60L);
                    LocalDateTime ini = LocalDateTime.of(agenda.fecha(), t);
                    LocalDateTime fin = ini.plusMinutes(duracion);
                    todasLasHoras.add(t);
                    if (!solapa(ini, fin, ac.ocupaciones())) {
                        libres.add(t);
                    }
                }
            }
            libresPorCancha.put(ac.cancha(), libres);
        }

        List<SlotDisponibilidad> resultado = new ArrayList<>();
        for (LocalTime hora : todasLasHoras) {
            boolean pasado = LocalDateTime.of(agenda.fecha(), hora).isBefore(ahora);
            List<CanchaLibre> libres = new ArrayList<>();
            if (!pasado) {
                for (Map.Entry<CanchaRef, TreeSet<LocalTime>> e : libresPorCancha.entrySet()) {
                    if (e.getValue().contains(hora)) {
                        CanchaRef c = e.getKey();
                        libres.add(new CanchaLibre(c.id(), c.nombre(), c.color(), c.techada(),
                                c.tipoPared(), c.precioHora()));
                    }
                }
            }
            resultado.add(new SlotDisponibilidad(hora, !libres.isEmpty(), libres));
        }
        return resultado;
    }

    private boolean solapa(LocalDateTime ini, LocalDateTime fin, List<Ocupacion> ocupaciones) {
        for (Ocupacion o : ocupaciones) {
            if (ini.isBefore(o.fin()) && o.inicio().isBefore(fin)) {
                return true;
            }
        }
        return false;
    }
}
