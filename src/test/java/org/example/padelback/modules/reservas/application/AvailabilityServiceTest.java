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
import org.example.padelback.modules.reservas.domain.model.disponibilidad.PrecioFranja;
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
                List.of(),
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

    @Test
    void franjaQueCierraAMedianoche_seTrataComoLasVeinticuatro_ultimoInicioEntraEnteroAntesDelCierre() {
        // M-medianoche: el front manda "00:00" como cierre de franja significando "24:00" (mismo día),
        // no la apertura. Con turno principal fino (30') anclado a las 20:00, la grilla llega hasta
        // 22:30 (22:30 + 90' = 00:00 exacto); antes del fix, "00:00" se leía como minuto 0 y la franja
        // quedaba vacía (0 slots) pese a tener 4hs reales de apertura.
        var agendaMedianoche = new AgendaDelDia(
                DIA, 30, List.of(60, 90, 120), 30, false,
                List.of(new Franja(LocalTime.of(20, 0), LocalTime.MIDNIGHT)),
                List.of(),
                List.of(new AgendaCancha(cancha(1, "C1"), List.of())));

        var slots = service.calcular(agendaMedianoche, 90, MEDIANOCHE);

        assertThat(slots).isNotEmpty();
        assertThat(slots.get(0).hora()).isEqualTo(LocalTime.of(20, 0));
        assertThat(ultimaHora(slots)).isEqualTo(LocalTime.of(22, 30));
        assertThat(slots).allMatch(SlotDisponibilidad::disponible);
    }

    // =====================================================================
    // Precio por horario (AvailabilityService#precioEfectivo): el precio de cada slot es el
    // EFECTIVO para su hora de inicio (franja de precio especial si aplica, si no el precio base
    // ya resuelto según el modo GENERAL/POR_CANCHA del complejo).
    // =====================================================================

    private static final BigDecimal PRECIO_BASE = new BigDecimal("12000.00");
    // Distinto del precio de la cancha() de prueba (8000.00) y de PRECIO_BASE, para que el test de
    // integración distinga sin ambigüedad qué precio ganó en cada slot.
    private static final BigDecimal PRECIO_FRANJA = new BigDecimal("5000.00");

    @Test
    void precioEfectivo_sinFranjas_devuelveElPrecioBase() {
        var precio = AvailabilityService.precioEfectivo(PRECIO_BASE, LocalTime.of(15, 0), List.of());

        assertThat(precio).isEqualByComparingTo(PRECIO_BASE);
    }

    @Test
    void precioEfectivo_inicioDentroDeLaFranja_devuelveElPrecioDeLaFranja() {
        var franjas = List.of(new PrecioFranja(LocalTime.of(15, 0), LocalTime.of(18, 0), PRECIO_FRANJA));

        var precio = AvailabilityService.precioEfectivo(PRECIO_BASE, LocalTime.of(16, 30), franjas);

        assertThat(precio).isEqualByComparingTo(PRECIO_FRANJA);
    }

    @Test
    void precioEfectivo_inicioFueraDeLaFranja_devuelveElPrecioBase() {
        var franjas = List.of(new PrecioFranja(LocalTime.of(15, 0), LocalTime.of(18, 0), PRECIO_FRANJA));

        var precio = AvailabilityService.precioEfectivo(PRECIO_BASE, LocalTime.of(20, 0), franjas);

        assertThat(precio).isEqualByComparingTo(PRECIO_BASE);
    }

    @Test
    void precioEfectivo_bordeDesde_esInclusivo() {
        var franjas = List.of(new PrecioFranja(LocalTime.of(15, 0), LocalTime.of(18, 0), PRECIO_FRANJA));

        var precio = AvailabilityService.precioEfectivo(PRECIO_BASE, LocalTime.of(15, 0), franjas);

        assertThat(precio).isEqualByComparingTo(PRECIO_FRANJA);
    }

    @Test
    void precioEfectivo_bordeHasta_esExclusivo() {
        var franjas = List.of(new PrecioFranja(LocalTime.of(15, 0), LocalTime.of(18, 0), PRECIO_FRANJA));

        var precio = AvailabilityService.precioEfectivo(PRECIO_BASE, LocalTime.of(18, 0), franjas);

        assertThat(precio).isEqualByComparingTo(PRECIO_BASE);
    }

    @Test
    void precioEfectivo_hastaMedianoche_seTrataComoLasVeinticuatro() {
        // M-medianoche: igual criterio que las franjas de apertura ("00:00" en hasta = 24:00).
        var franjas = List.of(new PrecioFranja(LocalTime.of(20, 0), LocalTime.MIDNIGHT, PRECIO_FRANJA));

        assertThat(AvailabilityService.precioEfectivo(PRECIO_BASE, LocalTime.of(23, 30), franjas))
                .isEqualByComparingTo(PRECIO_FRANJA);
        assertThat(AvailabilityService.precioEfectivo(PRECIO_BASE, LocalTime.of(19, 59), franjas))
                .isEqualByComparingTo(PRECIO_BASE);
    }

    @Test
    void calcular_aplicaElPrecioDeFranjaSoloAlSlotQueArrancaDentroDeElla() {
        var franjas = List.of(new PrecioFranja(LocalTime.of(15, 0), LocalTime.of(18, 0), PRECIO_FRANJA));
        var agendaConFranjaDePrecio = new AgendaDelDia(
                DIA, 30, List.of(60, 90, 120), 90, false,
                List.of(new Franja(LocalTime.of(8, 0), LocalTime.of(23, 0))),
                franjas,
                List.of(new AgendaCancha(cancha(1, "C1"), List.of())));

        var slots = service.calcular(agendaConFranjaDePrecio, 90, MEDIANOCHE);

        // Grilla cada 90' desde las 08:00: 08:00, 09:30, 11:00, 12:30, 14:00, 15:30 … 15:30 cae en la
        // franja [15:00,18:00), 08:00 no (queda con el precio propio de la cancha(), 8000.00).
        assertThat(slotAt(slots, LocalTime.of(8, 0)).canchasLibres().get(0).precioHora())
                .isEqualByComparingTo(new BigDecimal("8000.00"));
        assertThat(slotAt(slots, LocalTime.of(15, 30)).canchasLibres().get(0).precioHora())
                .isEqualByComparingTo(PRECIO_FRANJA);
    }
}
