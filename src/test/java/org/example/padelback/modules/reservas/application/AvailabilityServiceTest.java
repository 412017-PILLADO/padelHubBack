package org.example.padelback.modules.reservas.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.example.padelback.modules.reservas.domain.model.TipoPared;
import org.example.padelback.modules.reservas.domain.model.disponibilidad.AgendaCancha;
import org.example.padelback.modules.reservas.domain.model.disponibilidad.AgendaDelDia;
import org.example.padelback.modules.reservas.domain.model.disponibilidad.CanchaRef;
import org.example.padelback.modules.reservas.domain.model.disponibilidad.Franja;
import org.example.padelback.modules.reservas.domain.model.disponibilidad.Ocupacion;
import org.example.padelback.modules.reservas.domain.model.disponibilidad.SlotDisponibilidad;
import org.junit.jupiter.api.Test;

/**
 * Tests del cálculo de disponibilidad anclado al turno principal (función pura, sin Spring). Es el
 * caso de oro del dominio: la grilla de inicios se encadena de a {@code duracionDefault} (el turno
 * principal, típicamente 90') y <b>todas</b> las duraciones pedidas usan esa misma grilla como paso,
 * así un turno corto no desalinea la grilla del turno principal.
 */
class AvailabilityServiceTest {

    private static final LocalDate DIA = LocalDate.of(2030, 1, 1);
    private static final LocalDateTime MEDIANOCHE = DIA.atStartOfDay();
    private final AvailabilityService service = new AvailabilityService();

    private static CanchaRef cancha(long id, String nombre) {
        return new CanchaRef(id, nombre, "#2747ff", true, TipoPared.CRISTAL, new BigDecimal("8000.00"));
    }

    /** {@code principal} = turno principal del complejo (duracionDefault), que ancla la grilla. */
    private static AgendaDelDia agenda(int principal, List<AgendaCancha> canchas) {
        return new AgendaDelDia(
                DIA, 30, List.of(60, 90, 120), principal, false,
                List.of(new Franja(LocalTime.of(8, 0), LocalTime.of(23, 0))),
                canchas);
    }

    private static LocalTime ultimaHora(List<SlotDisponibilidad> slots) {
        return slots.get(slots.size() - 1).hora();
    }

    @Test
    void principal90_duracion90_iniciosCada90_ultimo2130() {
        var slots = service.calcular(
                agenda(90, List.of(new AgendaCancha(cancha(1, "C1"), List.of()))), 90, MEDIANOCHE);

        assertThat(slots.get(0).hora()).isEqualTo(LocalTime.of(8, 0));
        assertThat(slots.get(1).hora()).isEqualTo(LocalTime.of(9, 30));
        assertThat(slots).allMatch(SlotDisponibilidad::disponible).hasSize(10);
        assertThat(ultimaHora(slots)).isEqualTo(LocalTime.of(21, 30));
    }

    @Test
    void principal90_duracion60_seAnclaAGrillaDe90_noCada60() {
        var slots = service.calcular(
                agenda(90, List.of(new AgendaCancha(cancha(1, "C1"), List.of()))), 60, MEDIANOCHE);

        // La grilla la marca el turno principal (90), no la duración pedida: 08:00, 09:30, 11:00 …
        assertThat(slots.get(0).hora()).isEqualTo(LocalTime.of(8, 0));
        assertThat(slots.get(1).hora()).isEqualTo(LocalTime.of(9, 30));
        assertThat(slots).hasSize(10);
        assertThat(ultimaHora(slots)).isEqualTo(LocalTime.of(21, 30));
    }

    @Test
    void principal90_duracion120_consumeVariosSlotsYElCierreDejaResto() {
        var slots = service.calcular(
                agenda(90, List.of(new AgendaCancha(cancha(1, "C1"), List.of()))), 120, MEDIANOCHE);

        // Inicios cada 90 (08:00, 09:30 …) pero ventana 120: último que entra entero es 20:00.
        assertThat(slots.get(0).hora()).isEqualTo(LocalTime.of(8, 0));
        assertThat(slots.get(1).hora()).isEqualTo(LocalTime.of(9, 30));
        assertThat(ultimaHora(slots)).isEqualTo(LocalTime.of(20, 0));
    }

    @Test
    void elTurnoPrincipalConfigurableMarcaLaGrilla() {
        // Con turno principal 60, la grilla pasa a ser cada 60 (08:00, 09:00 … 22:00 = 15 inicios).
        var slots = service.calcular(
                agenda(60, List.of(new AgendaCancha(cancha(1, "C1"), List.of()))), 60, MEDIANOCHE);

        assertThat(slots).hasSize(15);
        assertThat(slots.get(1).hora()).isEqualTo(LocalTime.of(9, 0));
        assertThat(ultimaHora(slots)).isEqualTo(LocalTime.of(22, 0));
    }

    @Test
    void unaReservaBloqueaElSlotQueSolapaEnLaUnicaCancha() {
        var ocupacion = new Ocupacion(
                LocalDateTime.of(DIA, LocalTime.of(10, 0)), LocalDateTime.of(DIA, LocalTime.of(11, 0)));
        var slots = service.calcular(
                agenda(90, List.of(new AgendaCancha(cancha(1, "C1"), List.of(ocupacion)))), 60, MEDIANOCHE);

        // Grilla de 90 (08:00, 09:30, 11:00 …) con ventana 60: el inicio 09:30 → [09:30,10:30) solapa
        // la reserva [10:00,11:00) → no disponible; 08:00 y 11:00 (arranca al cerrar la reserva) sí.
        assertThat(slotAt(slots, LocalTime.of(9, 30)).disponible()).isFalse();
        assertThat(slotAt(slots, LocalTime.of(8, 0)).disponible()).isTrue();
        assertThat(slotAt(slots, LocalTime.of(11, 0)).disponible()).isTrue();
    }

    @Test
    void conDosCanchasElSlotSigueDisponibleSiUnaEstaLibre() {
        var ocupacion = new Ocupacion(
                LocalDateTime.of(DIA, LocalTime.of(9, 30)), LocalDateTime.of(DIA, LocalTime.of(10, 30)));
        var slots = service.calcular(agenda(90, List.of(
                new AgendaCancha(cancha(1, "C1"), List.of(ocupacion)),
                new AgendaCancha(cancha(2, "C2"), List.of()))), 60, MEDIANOCHE);

        var nueveYMedia = slotAt(slots, LocalTime.of(9, 30));
        assertThat(nueveYMedia.disponible()).isTrue();
        assertThat(nueveYMedia.canchasLibres()).extracting(c -> c.id()).containsExactly(2L);
    }

    @Test
    void inicioPasadoQuedaNoDisponible() {
        var ahora = LocalDateTime.of(DIA, LocalTime.of(12, 0));
        var slots = service.calcular(
                agenda(90, List.of(new AgendaCancha(cancha(1, "C1"), List.of()))), 60, ahora);

        assertThat(slotAt(slots, LocalTime.of(11, 0)).disponible()).isFalse();
        assertThat(slotAt(slots, LocalTime.of(12, 30)).disponible()).isTrue();
    }

    private static SlotDisponibilidad slotAt(List<SlotDisponibilidad> slots, LocalTime hora) {
        return slots.stream().filter(s -> s.hora().equals(hora)).findFirst().orElseThrow();
    }
}
