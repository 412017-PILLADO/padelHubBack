package org.example.padelback.modules.pagos.infrastructure;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.example.padelback.infrastructure.config.MercadoPagoProperties;
import org.example.padelback.modules.pagos.domain.model.PagoMp;
import org.example.padelback.modules.pagos.domain.model.PreferenciaSena;
import org.example.padelback.modules.pagos.domain.model.TokensMp;
import org.example.padelback.modules.pagos.domain.port.MercadoPagoGatewayPort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Adapter HTTP a la API de Mercado Pago (RestClient, sin SDK). Los errores 4xx/5xx de MP
 * burbujean como RestClientResponseException; los use cases deciden si degradan o propagan.
 */
@Component
public class MercadoPagoClient implements MercadoPagoGatewayPort {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxxx");

    private final MercadoPagoProperties props;
    private final RestClient http;
    private final Clock clock;

    public MercadoPagoClient(MercadoPagoProperties props, RestClient.Builder builder, Clock clock) {
        this.props = props;
        this.http = builder.build();
        this.clock = clock;
    }

    @Override
    public TokensMp intercambiarCode(String code) {
        Map<String, Object> body = Map.of(
                "client_id", props.clientId(),
                "client_secret", props.clientSecret(),
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", props.redirectUri());
        return toTokens(postJson(props.apiBase() + "/oauth/token", null, body));
    }

    @Override
    public TokensMp refrescar(String refreshToken) {
        Map<String, Object> body = Map.of(
                "client_id", props.clientId(),
                "client_secret", props.clientSecret(),
                "grant_type", "refresh_token",
                "refresh_token", refreshToken);
        return toTokens(postJson(props.apiBase() + "/oauth/token", null, body));
    }

    @Override
    public PreferenciaSena crearPreferencia(String accessToken, String titulo, BigDecimal monto,
                                            String externalReference, String notificationUrl,
                                            LocalDateTime expiraEn, String backUrl) {
        Map<String, Object> body = new HashMap<>();
        body.put("items", List.of(Map.of(
                "title", titulo,
                "quantity", 1,
                "unit_price", monto,
                "currency_id", "ARS")));
        body.put("external_reference", externalReference);
        if (notificationUrl != null && !notificationUrl.isBlank()) {
            body.put("notification_url", notificationUrl);
        }
        if (expiraEn != null) {
            // La preferencia vence junto con la reserva PENDIENTE: pagado tarde = link muerto.
            body.put("expires", true);
            body.put("expiration_date_to",
                    ZonedDateTime.of(expiraEn, clock.getZone()).format(ISO_OFFSET));
        } else {
            body.put("expires", true);
        }
        if (backUrl != null && !backUrl.isBlank()) {
            body.put("back_urls", Map.of("success", backUrl, "pending", backUrl, "failure", backUrl));
        }
        Map<String, Object> resp = postJson(props.apiBase() + "/checkout/preferences", accessToken, body);
        return new PreferenciaSena(String.valueOf(resp.get("id")), (String) resp.get("init_point"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public PagoMp consultarPago(String accessToken, String paymentId) {
        Map<String, Object> resp = http.get()
                .uri(props.apiBase() + "/v1/payments/" + paymentId)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);
        return new PagoMp(String.valueOf(resp.get("id")), (String) resp.get("status"),
                (String) resp.get("external_reference"),
                new BigDecimal(String.valueOf(resp.get("transaction_amount"))));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> postJson(String url, String bearer, Map<String, Object> body) {
        RestClient.RequestBodySpec spec = http.post().uri(url).contentType(MediaType.APPLICATION_JSON);
        if (bearer != null) {
            spec = spec.header("Authorization", "Bearer " + bearer);
        }
        return spec.body(body).retrieve().body(Map.class);
    }

    private static TokensMp toTokens(Map<String, Object> r) {
        return new TokensMp(
                (String) r.get("access_token"),
                (String) r.get("refresh_token"),
                String.valueOf(r.get("user_id")),
                (String) r.get("public_key"),
                (String) r.get("scope"),
                Long.parseLong(String.valueOf(r.get("expires_in"))));
    }
}
