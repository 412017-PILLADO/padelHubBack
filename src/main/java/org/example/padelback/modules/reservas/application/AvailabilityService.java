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
 * Cálculo de disponibilidad anclado al <b>turno principal</b> con duración variable (función pura).
 * La grilla de inicios se encadena de a {@code duracionDefault} (el turno principal del complejo,
 * típicamente 90') desde la apertura de cada franja: 08:00 → 08:00+principal → 08:00+2·principal …
 * <b>Todas</b> las duraciones pedidas (60/90/120) usan esa misma grilla como paso, de modo que un
 * turno corto no desalinea la grilla del turno principal ("uno sale, entra el siguiente, siempre en
 * horario redondo de 90'"). El {@code pasoMinutos} del complejo quedó vestigial y no influye.
 *
 * <p>Una cancha está libre en el inicio {@code T} si {@code [T, T+duracion)} entra dentro de una
 * franja de apertura y no solapa ninguna de sus ocupaciones (reservas confirmadas + bloqueos). Un
 * slot es {@code disponible} si al menos una cancha está libre, y devuelve todas las libres para que
 * el cliente pueda elegir una o dejar que el back asigne ("cualquiera").
 *
 * <p>Como la grilla queda anclada al turno principal, una duración pedida mayor al principal (ej.
 * 120 con principal 90) consume varios slots de grilla, y el cierre puede dejar un resto menor a
 * {@code duracion} sin turno: es inherente al modelo cuando el largo de la franja no es múltiplo del
 * paso elegido.
 */
@Service
public class AvailabilityService {

    /**
     * @param agenda  snapshot del día (franjas compartidas + canchas con sus ocupaciones)
     * @param duracion duración pedida en minutos (ya validada contra las permitidas)
     * @param ahora    hora de pared del negocio; los inicios ya pasados se devuelven no disponibles
     */
    public List<SlotDisponibilidad> calcular(AgendaDelDia agenda, int duracion, LocalDateTime ahora) {
        // El paso de la grilla es el turno principal; nunca menor a la duración mínima razonable.
        int paso = Math.max(1, agenda.duracionDefault());
        TreeSet<LocalTime> todasLasHoras = new TreeSet<>();
        // cancha -> inicios libres (orden de inserción para reflejar el orden de canchas)
        Map<CanchaRef, TreeSet<LocalTime>> libresPorCancha = new LinkedHashMap<>();

        for (AgendaCancha ac : agenda.canchas()) {
            TreeSet<LocalTime> libres = new TreeSet<>();
            for (Franja f : agenda.franjas()) {
                // Aritmética en minutos del día: evita el wrap de LocalTime.plusMinutes a medianoche
                // (con franja que cierra 23:00 y un inicio que cae justo ahí, T+duracion daría 00:00
                // y el bucle nunca terminaría). El paso del encadenado es el turno principal: la
                // grilla de inicios es siempre la del turno principal y la ventana es la duración
                // pedida. El último inicio es el que entra entero (con su duración) en la franja.
                int inicioMin = f.inicio().toSecondOfDay() / 60;
                // Cierre a medianoche: el front manda "00:00" para "cierra a las 24:00" (mismo día),
                // no para "abre a las 00:00". Sin este caso especial, toSecondOfDay()/60 daría 0 y la
                // franja quedaría con 0 minutos de largo (inicioMin > finMin, sin turnos).
                int finMin = f.fin().equals(LocalTime.MIDNIGHT) ? 24 * 60 : f.fin().toSecondOfDay() / 60;
                for (int m = inicioMin; m + duracion <= finMin; m += paso) {
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
