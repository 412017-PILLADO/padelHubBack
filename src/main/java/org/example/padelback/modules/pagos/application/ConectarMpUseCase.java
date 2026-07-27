package org.example.padelback.modules.pagos.application;

import java.time.Clock;
import java.util.Optional;

import org.example.padelback.domain.port.TenantProvider;
import org.example.padelback.infrastructure.config.MercadoPagoProperties;
import org.example.padelback.infrastructure.tenancy.TenantContext;
import org.example.padelback.modules.pagos.domain.exception.MpCallbackInvalidoException;
import org.example.padelback.modules.pagos.domain.exception.MpNoConfiguradoException;
import org.example.padelback.modules.pagos.domain.model.CredencialMp;
import org.example.padelback.modules.pagos.domain.model.TokensMp;
import org.example.padelback.modules.pagos.domain.port.CredencialMpStorePort;
import org.example.padelback.modules.pagos.domain.port.MercadoPagoGatewayPort;
import org.example.padelback.modules.pagos.infrastructure.crypto.MpOAuthStateCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConectarMpUseCase {

    /** El returnTo viene del front: solo se acepta una URL http(s) que termine en el panel admin. */
    private static final String RETURN_TO_PATTERN = "^https?://[^/\\s]+/admin(/.*)?$";

    private final MercadoPagoProperties props;
    private final MpOAuthStateCodec stateCodec;
    private final MercadoPagoGatewayPort gateway;
    private final CredencialMpStorePort store;
    private final TenantProvider tenantProvider;
    private final Clock clock;

    /** URL de autorización de MP a la que el panel redirige al dueño. */
    public String generarUrlAutorizacion(String returnTo) {
        if (!props.configurado()) {
            throw new MpNoConfiguradoException();
        }
        if (returnTo == null || !returnTo.matches(RETURN_TO_PATTERN)) {
            throw new IllegalArgumentException("returnTo inválido");
        }
        String state = stateCodec.crear(tenantProvider.requireTenantId(), returnTo);
        return props.authBase() + "/authorization"
                + "?client_id=" + props.clientId()
                + "&response_type=code&platform_id=mp"
                + "&state=" + state
                + "&redirect_uri=" + java.net.URLEncoder.encode(props.redirectUri(), java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Callback público (sin JWT ni X-Tenant): el tenant viene FIRMADO dentro del state.
     * Devuelve el returnTo para que el controller redirija al panel.
     */
    public String procesarCallback(String code, String state) {
        if (code == null || code.isBlank()) {
            throw new MpCallbackInvalidoException("code ausente (el dueño canceló la autorización)");
        }
        MpOAuthStateCodec.StateData data = stateCodec.validar(state);
        TokensMp tokens = gateway.intercambiarCode(code);
        TenantContext.runAs(data.tenantId(), () -> {
            store.guardar(new CredencialMp(data.tenantId(), tokens.mpUserId(), tokens.accessToken(),
                            tokens.refreshToken(), clock.instant().plusSeconds(tokens.expiresInSegundos())),
                    tokens.mpPublicKey(), tokens.scope());
            return null;
        });
        return data.returnTo();
    }

    public Optional<CredencialMp> estado() {
        return store.cargar(tenantProvider.requireTenantId());
    }

    public void desconectar() {
        store.eliminar(tenantProvider.requireTenantId());
    }
}
