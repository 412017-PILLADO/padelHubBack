package org.example.padelback.modules.pagos.domain.model;

import java.time.Instant;

/** Credencial OAuth de MP de un tenant. Los tokens viajan EN CLARO acá; el store cifra al persistir. */
public record CredencialMp(
        long tenantId,
        String mpUserId,
        String accessToken,
        String refreshToken,
        Instant expiraEn) {

    public boolean vigente(Instant ahora) {
        return expiraEn.isAfter(ahora);
    }
}
