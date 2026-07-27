package org.example.padelback.modules.pagos.domain.model;

import java.math.BigDecimal;

/** Pago consultado en /v1/payments/{id} con el token del tenant. */
public record PagoMp(String id, String status, String externalReference, BigDecimal transactionAmount) {}
