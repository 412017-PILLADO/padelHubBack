package org.example.padelback.modules.pagos.infrastructure.scheduling;

import java.time.Clock;
import java.time.temporal.ChronoUnit;

import org.example.padelback.modules.pagos.domain.model.CredencialMp;
import org.example.padelback.modules.pagos.domain.model.TokensMp;
import org.example.padelback.modules.pagos.domain.port.CredencialMpStorePort;
import org.example.padelback.modules.pagos.domain.port.MercadoPagoGatewayPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Los access tokens OAuth de MP duran 180 días: este job renueva (con el refresh token) los que
 * vencen dentro de 30 días. Si el refresh falla se loguea y se reintenta al día siguiente; si el
 * token llegó a vencer, /estado devuelve conectado=false y el panel ofrece reconectar.
 */
@Component
public class MpTokenRefreshJob {

    private static final Logger log = LoggerFactory.getLogger(MpTokenRefreshJob.class);
    private static final int DIAS_ANTES = 30;

    private final CredencialMpStorePort store;
    private final MercadoPagoGatewayPort gateway;
    private final Clock clock;

    public MpTokenRefreshJob(CredencialMpStorePort store, MercadoPagoGatewayPort gateway, Clock clock) {
        this.store = store;
        this.gateway = gateway;
        this.clock = clock;
    }

    @Scheduled(cron = "${padel.mercadopago.refresh-cron:0 0 4 * * *}", zone = "UTC")
    public void refrescarVencimientosProximos() {
        for (CredencialMp cred : store.conVencimientoAntesDe(clock.instant().plus(DIAS_ANTES, ChronoUnit.DAYS))) {
            try {
                TokensMp t = gateway.refrescar(cred.refreshToken());
                store.guardar(new CredencialMp(cred.tenantId(), t.mpUserId(), t.accessToken(),
                        t.refreshToken(), clock.instant().plusSeconds(t.expiresInSegundos())),
                        t.mpPublicKey(), t.scope());
                log.info("Token MP refrescado para tenant {}", cred.tenantId());
            } catch (RuntimeException e) {
                log.warn("No se pudo refrescar el token MP del tenant {}: {}", cred.tenantId(), e.getMessage());
            }
        }
    }
}
