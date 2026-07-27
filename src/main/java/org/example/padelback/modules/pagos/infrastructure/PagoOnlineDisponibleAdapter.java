package org.example.padelback.modules.pagos.infrastructure;

import java.time.Clock;

import org.example.padelback.modules.pagos.domain.port.CredencialMpStorePort;
import org.example.padelback.modules.reservas.domain.port.PagoOnlineQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PagoOnlineDisponibleAdapter implements PagoOnlineQuery {

    private final CredencialMpStorePort store;
    private final Clock clock;

    @Override
    public boolean disponible(long tenantId) {
        return store.cargar(tenantId).map(c -> c.vigente(clock.instant())).orElse(false);
    }
}
