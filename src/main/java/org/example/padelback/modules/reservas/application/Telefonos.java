package org.example.padelback.modules.reservas.application;

/** Normaliza teléfonos AR para comparar identidad: solo dígitos, últimos 10 (área + número). */
public final class Telefonos {
    private Telefonos() {}

    public static String normalizar(String raw) {
        if (raw == null) return "";
        String soloDigitos = raw.replaceAll("\\D", "");
        return soloDigitos.length() <= 10 ? soloDigitos : soloDigitos.substring(soloDigitos.length() - 10);
    }
}
