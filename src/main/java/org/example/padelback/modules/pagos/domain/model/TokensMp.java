package org.example.padelback.modules.pagos.domain.model;

/** Respuesta del intercambio/refresh OAuth de MP. */
public record TokensMp(
        String accessToken,
        String refreshToken,
        String mpUserId,
        String mpPublicKey,
        String scope,
        long expiresInSegundos) {}
