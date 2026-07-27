package org.example.padelback.modules.pagos.presentation.dto;

import java.time.Instant;
import java.util.Optional;

import org.example.padelback.modules.pagos.domain.model.CredencialMp;

public record MpEstadoResponse(boolean conectado, String mpUserId, String expiraEn) {

    public static MpEstadoResponse from(Optional<CredencialMp> cred, Instant ahora) {
        return cred.filter(c -> c.vigente(ahora))
                .map(c -> new MpEstadoResponse(true, c.mpUserId(), c.expiraEn().toString()))
                .orElse(new MpEstadoResponse(false, null, null));
    }
}
