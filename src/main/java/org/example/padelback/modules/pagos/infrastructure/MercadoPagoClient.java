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
        body.put("expires", true);
        if (expiraEn != null) {
            // La preferencia vence junto con la reserva PENDIENTE: pagado tarde = link muerto.
            body.put("expiration_date_to",
                    ZonedDateTime.of(expiraEn, clock.getZone()).format(ISO_OFFSET));
        }
        if (backUrl != null && !backUrl.isBlank()) {
            body.put("back_urls", Map.of("success", backUrl, "pending", backUrl, "failure", backUrl));
        }
        Map<String, Object> resp = postJson(props.apiBase() + "/checkout/preferences", accessToken, body);
        return new PreferenciaSena(campoRequerido(resp, "id"), campoRequerido(resp, "init_point"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public PagoMp consultarPago(String accessToken, String paymentId) {
        Map<String, Object> resp = http.get()
                .uri(props.apiBase() + "/v1/payments/" + paymentId)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);
        return new PagoMp(campoRequerido(resp, "id"), campoRequerido(resp, "status"),
                (String) resp.get("external_reference"),
                new BigDecimal(campoRequerido(resp, "transaction_amount")));
    }

    @Override
    public void reembolsarPago(String accessToken, String paymentId) {
        http.post()
                .uri(props.apiBase() + "/v1/payments/" + paymentId + "/refunds")
                .header("Authorization", "Bearer " + accessToken)
                // MP exige idempotency key en refunds: reintentar no duplica la devolución.
                .header("X-Idempotency-Key", "refund-" + paymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of())
                .retrieve()
                .toBodilessEntity();
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
                campoRequerido(r, "access_token"),
                (String) r.get("refresh_token"),
                campoRequerido(r, "user_id"),
                (String) r.get("public_key"),
                (String) r.get("scope"),
                Long.parseLong(campoRequerido(r, "expires_in")));
    }

    /** Campo obligatorio de una respuesta de MP: falla con mensaje claro si falta (no NumberFormatException críptica). */
    private static String campoRequerido(Map<String, Object> resp, String campo) {
        if (resp == null || resp.get(campo) == null) {
            throw new IllegalStateException("Respuesta de Mercado Pago sin campo requerido: " + campo);
        }
        return String.valueOf(resp.get(campo));
    }
}
