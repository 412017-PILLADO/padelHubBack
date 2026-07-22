package org.example.padelback.modules.reservas.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;

import org.example.padelback.modules.reservas.domain.model.disponibilidad.Franja;
import org.junit.jupiter.api.Test;

/**
 * Tests de {@link AgendaConfigCommandAdapter#franjasDelDia} (función pura, sin persistencia): la
 * traducción de un día de {@code guardarHorarios} a las franjas efectivas, incluyendo la intersección
 * real del break (A1) y el cierre a medianoche (M-medianoche).
 */
class AgendaConfigCommandAdapterTest {

    @Test
    void sinBreak_devuelveUnaSolaFranja() {
        var franjas = AgendaConfigCommandAdapter.franjasDelDia(LocalTime.of(8, 0), LocalTime.of(23, 0), false, null, null);

        assertThat(franjas).containsExactly(new Franja(LocalTime.of(8, 0), LocalTime.of(23, 0)));
    }

    @Test
    void breakCompletoDentroDeLaFranja_partaEnDos() {
        var franjas = AgendaConfigCommandAdapter.franjasDelDia(LocalTime.of(8, 0), LocalTime.of(23, 0), true,
                LocalTime.of(13, 0), LocalTime.of(16, 0));

        assertThat(franjas).containsExactly(
                new Franja(LocalTime.of(8, 0), LocalTime.of(13, 0)),
                new Franja(LocalTime.of(16, 0), LocalTime.of(23, 0)));
    }

    @Test
    void breakQueArrancaJustoEnLaApertura_soloQuedaLaTarde() {
        // Antes (bug A1): al no caer COMPLETO dentro de la franja (breakFrom no es > from), el break
        // se ignoraba entero y la franja quedaba abierta 08:00-23:00 completa (vendiendo el "descanso").
        var franjas = AgendaConfigCommandAdapter.franjasDelDia(LocalTime.of(8, 0), LocalTime.of(23, 0), true,
                LocalTime.of(8, 0), LocalTime.of(10, 0));

        assertThat(franjas).containsExactly(new Franja(LocalTime.of(10, 0), LocalTime.of(23, 0)));
    }

    @Test
    void breakQueTerminaJustoEnElCierre_soloQuedaLaManana() {
        var franjas = AgendaConfigCommandAdapter.franjasDelDia(LocalTime.of(8, 0), LocalTime.of(23, 0), true,
                LocalTime.of(21, 0), LocalTime.of(23, 0));

        assertThat(franjas).containsExactly(new Franja(LocalTime.of(8, 0), LocalTime.of(21, 0)));
    }

    @Test
    void breakQueExcedePorAmbosLados_seRecortaALaFranja_yLaCubreEntera() {
        // El break declarado (07:00-23:30) excede la franja (08:00-23:00) por los dos lados: la
        // intersección real es la franja completa, así que no queda ninguna sub-franja.
        var franjas = AgendaConfigCommandAdapter.franjasDelDia(LocalTime.of(8, 0), LocalTime.of(23, 0), true,
                LocalTime.of(7, 0), LocalTime.of(23, 30));

        assertThat(franjas).isEmpty();
    }

    @Test
    void breakFueraDeLaFranja_noAfecta() {
        var franjas = AgendaConfigCommandAdapter.franjasDelDia(LocalTime.of(8, 0), LocalTime.of(12, 0), true,
                LocalTime.of(13, 0), LocalTime.of(16, 0));

        assertThat(franjas).containsExactly(new Franja(LocalTime.of(8, 0), LocalTime.of(12, 0)));
    }

    @Test
    void breakOnFalse_ignoraElBreakAunqueVengaCargado() {
        var franjas = AgendaConfigCommandAdapter.franjasDelDia(LocalTime.of(8, 0), LocalTime.of(23, 0), false,
                LocalTime.of(13, 0), LocalTime.of(16, 0));

        assertThat(franjas).containsExactly(new Franja(LocalTime.of(8, 0), LocalTime.of(23, 0)));
    }

    @Test
    void cierreAMedianoche_seInterpretaComo24hs_sinBreak() {
        var franjas = AgendaConfigCommandAdapter.franjasDelDia(LocalTime.of(20, 0), LocalTime.MIDNIGHT, false, null, null);

        assertThat(franjas).containsExactly(new Franja(LocalTime.of(20, 0), LocalTime.MIDNIGHT));
    }

    @Test
    void cierreAMedianoche_conBreakQueTerminaEnElCierre_soloQuedaLaFranjaDeAntesDelBreak() {
        var franjas = AgendaConfigCommandAdapter.franjasDelDia(LocalTime.of(16, 0), LocalTime.MIDNIGHT, true,
                LocalTime.of(21, 0), LocalTime.MIDNIGHT);

        assertThat(franjas).containsExactly(new Franja(LocalTime.of(16, 0), LocalTime.of(21, 0)));
    }

    @Test
    void fromIgualATo_esInvalida() {
        assertThatThrownBy(() -> AgendaConfigCommandAdapter.franjasDelDia(LocalTime.of(8, 0), LocalTime.of(8, 0), false, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fromYToAmbosMedianoche_representaUnaFranjaDe24Horas() {
        // from=00:00 se lee como apertura real a medianoche (minuto 0) y to=00:00 como cierre a las
        // 24:00 (minuto 1440): no colisionan, describen un día abierto las 24hs.
        var franjas = AgendaConfigCommandAdapter.franjasDelDia(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT, false, null, null);

        assertThat(franjas).containsExactly(new Franja(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT));
    }
}
