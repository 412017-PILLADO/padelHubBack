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
 * Tests del cálculo de disponibilidad de turnos contiguos con duración variable (función pura, sin
 * Spring). Es el caso de oro del dominio: los inicios se encadenan de a {@code duracion} desde la
 * apertura (uno sale, entra el siguiente); el {@code paso} del complejo ya no influye.
 */
class AvailabilityServiceTest {

    private static final LocalDate DIA = LocalDate.of(2030, 1, 1);
    private static final LocalDateTime MEDIANOCHE = DIA.atStartOfDay();
    private final AvailabilityService service = new AvailabilityService();

    private static CanchaRef cancha(long id, String nombre) {
        return new CanchaRef(id, nombre, "#2747ff", true, TipoPared.CRISTAL, new BigDecimal("8000.00"));
    }

    private static AgendaDelDia agenda(int paso, List<AgendaCancha> canchas) {
        return new AgendaDelDia(
                DIA, paso, List.of(60, 90, 120), 90,
                List.of(new Franja(LocalTime.of(8, 0), LocalTime.of(23, 0))),
                canchas);
    }

    private static LocalTime ultimaHora(List<SlotDisponibilidad> slots) {
        return slots.get(slots.size() - 1).hora();
    }

    @Test
    void duracion60_ultimoInicioEsLas22() {
        var slots = service.calcular(
                agenda(30, List.of(new AgendaCancha(cancha(1, "C1"), List.of()))), 60, MEDIANOCHE);

        assertThat(slots.get(0).hora()).isEqualTo(LocalTime.of(8, 0));
        assertThat(slots).allMatch(SlotDisponibilidad::disponible);
        assertThat(ultimaHora(slots)).isEqualTo(LocalTime.of(22, 0));
    }

    @Test
    void duracion90_ultimoInicioEsLas2130() {
        var slots = service.calcular(
                agenda(30, List.of(new AgendaCancha(cancha(1, "C1"), List.of()))), 90, MEDIANOCHE);

        assertThat(ultimaHora(slots)).isEqualTo(LocalTime.of(21, 30));
    }

    @Test
    void duracion120_encadenaCada120YElCierreDejaRestoLibre() {
        var slots = service.calcular(
                agenda(30, List.of(new AgendaCancha(cancha(1, "C1"), List.of()))), 120, MEDIANOCHE);

        // 08:00 → 10:00 → … → 20:00 (último que entra entero); 22:00-23:00 queda sin turno.
        assertThat(slots.get(0).hora()).isEqualTo(LocalTime.of(8, 0));
        assertThat(slots).hasSize(7);
        assertThat(ultimaHora(slots)).isEqualTo(LocalTime.of(20, 0));
    }

    @Test
    void elPasoYaNoAfectaLaGranularidad_losIniciosVanCadaDuracion() {
        var conPaso30 = service.calcular(
                agenda(30, List.of(new AgendaCancha(cancha(1, "C1"), List.of()))), 60, MEDIANOCHE);
        var conPaso90 = service.calcular(
                agenda(90, List.of(new AgendaCancha(cancha(1, "C1"), List.of()))), 60, MEDIANOCHE);

        // Con duración 60 los inicios se encadenan cada 60 (08:00, 09:00 … 22:00 = 15), sin importar el paso.
        assertThat(conPaso30).hasSize(15);
        assertThat(conPaso30.get(1).hora()).isEqualTo(LocalTime.of(9, 0));
        assertThat(conPaso30.stream().map(SlotDisponibilidad::hora).toList())
                .isEqualTo(conPaso90.stream().map(SlotDisponibilidad::hora).toList());
    }

    @Test
    void unaReservaBloqueaElSlotQueSolapaEnLaUnicaCancha() {
        var ocupacion = new Ocupacion(
                LocalDateTime.of(DIA, LocalTime.of(10, 0)), LocalDateTime.of(DIA, LocalTime.of(11, 0)));
        var slots = service.calcular(
                agenda(30, List.of(new AgendaCancha(cancha(1, "C1"), List.of(ocupacion)))), 60, MEDIANOCHE);

        // Inicios encadenados cada 60 (08:00, 09:00 …): el de las 10:00 solapa [10:00,11:00) → no
        // disponible; 09:00 (termina justo 10:00) y 11:00 (arranca justo al cerrar la reserva) sí.
        assertThat(slotAt(slots, LocalTime.of(10, 0)).disponible()).isFalse();
        assertThat(slotAt(slots, LocalTime.of(9, 0)).disponible()).isTrue();
        assertThat(slotAt(slots, LocalTime.of(11, 0)).disponible()).isTrue();
    }

    @Test
    void conDosCanchasElSlotSigueDisponibleSiUnaEstaLibre() {
        var ocupacion = new Ocupacion(
                LocalDateTime.of(DIA, LocalTime.of(10, 0)), LocalDateTime.of(DIA, LocalTime.of(11, 0)));
        var slots = service.calcular(agenda(30, List.of(
                new AgendaCancha(cancha(1, "C1"), List.of(ocupacion)),
                new AgendaCancha(cancha(2, "C2"), List.of()))), 60, MEDIANOCHE);

        var diez = slotAt(slots, LocalTime.of(10, 0));
        assertThat(diez.disponible()).isTrue();
        assertThat(diez.canchasLibres()).extracting(c -> c.id()).containsExactly(2L);
    }

    @Test
    void inicioPasadoQuedaNoDisponible() {
        var ahora = LocalDateTime.of(DIA, LocalTime.of(12, 0));
        var slots = service.calcular(
                agenda(30, List.of(new AgendaCancha(cancha(1, "C1"), List.of()))), 60, ahora);

        assertThat(slotAt(slots, LocalTime.of(9, 0)).disponible()).isFalse();
        assertThat(slotAt(slots, LocalTime.of(12, 0)).disponible()).isTrue();
    }

    private static SlotDisponibilidad slotAt(List<SlotDisponibilidad> slots, LocalTime hora) {
        return slots.stream().filter(s -> s.hora().equals(hora)).findFirst().orElseThrow();
    }
}
