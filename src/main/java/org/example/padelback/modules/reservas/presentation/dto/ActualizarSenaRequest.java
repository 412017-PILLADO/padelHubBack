package org.example.padelback.modules.reservas.presentation.dto;

import java.math.BigDecimal;

/**
 * @param requiereSena activa/desactiva el módulo de señas
 * @param senaMonto    monto de la seña (obligatorio y &gt; 0 cuando requiereSena es true; puede ser null si false)
 * @param senaAlias    alias/CBU donde el cliente transfiere (obligatorio cuando requiereSena es true)
 */
public record ActualizarSenaRequest(
        boolean requiereSena,
        BigDecimal senaMonto,
        String senaAlias) {}
