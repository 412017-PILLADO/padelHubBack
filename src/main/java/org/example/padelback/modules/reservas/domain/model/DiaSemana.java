package org.example.padelback.modules.reservas.domain.model;

/** 0=LUNES ... 6=DOMINGO (alineado con java.time.DayOfWeek.getValue()-1). */
public enum DiaSemana {
    LUNES(0), MARTES(1), MIERCOLES(2), JUEVES(3), VIERNES(4), SABADO(5), DOMINGO(6);

    private final int indice;

    DiaSemana(int indice) {
        this.indice = indice;
    }

    public int indice() {
        return indice;
    }
}
