package org.example.padelback.modules.reservas.domain.model;

/**
 * Cómo cobra el complejo la hora de cancha.
 *
 * <ul>
 *   <li>{@link #GENERAL}: un único precio por hora para todo el complejo (se ignora el de cada
 *       cancha). Útil cuando todas las canchas valen lo mismo.</li>
 *   <li>{@link #POR_CANCHA}: cada cancha define su propio precio por hora (para distinguir con
 *       luz/sin luz, cemento/cristal, techada/descubierta, etc.).</li>
 * </ul>
 */
public enum PrecioModo {
    GENERAL,
    POR_CANCHA
}
