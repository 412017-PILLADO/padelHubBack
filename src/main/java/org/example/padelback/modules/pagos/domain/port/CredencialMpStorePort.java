package org.example.padelback.modules.pagos.domain.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.example.padelback.modules.pagos.domain.model.CredencialMp;

public interface CredencialMpStorePort {

    Optional<CredencialMp> cargar(long tenantId);

    /** Alta o reemplazo (reconexión) de la credencial del tenant. */
    void guardar(CredencialMp credencial, String mpPublicKey, String scope);

    void eliminar(long tenantId);

    /** Credenciales cuyo access token vence antes de {@code limite} (para el job de refresh). */
    List<CredencialMp> conVencimientoAntesDe(Instant limite);
}
