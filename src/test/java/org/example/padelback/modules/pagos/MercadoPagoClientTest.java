package org.example.padelback.modules.pagos;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.example.padelback.infrastructure.config.MercadoPagoProperties;
import org.example.padelback.modules.pagos.domain.model.PagoMp;
import org.example.padelback.modules.pagos.domain.model.PreferenciaSena;
import org.example.padelback.modules.pagos.domain.model.TokensMp;
import org.example.padelback.modules.pagos.infrastructure.MercadoPagoClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MercadoPagoClientTest {

    private static final MercadoPagoProperties PROPS = new MercadoPagoProperties(
            "APP-123", "secret-xyz", "http://localhost:8095/public/pagos/mp/oauth/callback",
            "", "https://api.mp.test", "https://auth.mp.test");

    private MockRestServiceServer server;
    private MercadoPagoClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new MercadoPagoClient(PROPS, builder,
                Clock.system(ZoneId.of("America/Argentina/Cordoba")));
    }

    @Test
    void intercambiaCodePorTokens() {
        server.expect(requestTo("https://api.mp.test/oauth/token"))
                .andExpect(jsonPath("$.client_id").value("APP-123"))
                .andExpect(jsonPath("$.grant_type").value("authorization_code"))
                .andExpect(jsonPath("$.code").value("TG-abc"))
                .andRespond(withSuccess("""
                        {"access_token":"APP_USR-tok","token_type":"Bearer","expires_in":15552000,
                         "scope":"offline_access read write","user_id":222,
                         "refresh_token":"TG-ref","public_key":"APP_PUB"}
                        """, MediaType.APPLICATION_JSON));

        TokensMp t = client.intercambiarCode("TG-abc");
        assertEquals("APP_USR-tok", t.accessToken());
        assertEquals("TG-ref", t.refreshToken());
        assertEquals("222", t.mpUserId());
        assertEquals(15552000L, t.expiresInSegundos());
    }

    @Test
    void creaPreferenciaConTokenDelVendedor() {
        server.expect(requestTo("https://api.mp.test/checkout/preferences"))
                .andExpect(header("Authorization", "Bearer TOKEN-VENDEDOR"))
                .andExpect(jsonPath("$.items[0].unit_price").value(5000.0))
                .andExpect(jsonPath("$.items[0].currency_id").value("ARS"))
                .andExpect(jsonPath("$.external_reference").value("1-42"))
                .andExpect(jsonPath("$.expires").value(true))
                .andRespond(withSuccess("""
                        {"id":"pref-99","init_point":"https://mp/checkout/pref-99"}
                        """, MediaType.APPLICATION_JSON));

        PreferenciaSena p = client.crearPreferencia("TOKEN-VENDEDOR", "Seña — Club Demo",
                new BigDecimal("5000.00"), "1-42", null,
                LocalDateTime.of(2026, 8, 1, 12, 0), "http://demo.localhost:4400");
        assertEquals("pref-99", p.preferenceId());
        assertEquals("https://mp/checkout/pref-99", p.initPoint());
    }

    @Test
    void consultaPago() {
        server.expect(requestTo("https://api.mp.test/v1/payments/555"))
                .andExpect(header("Authorization", "Bearer TOKEN-VENDEDOR"))
                .andRespond(withSuccess("""
                        {"id":555,"status":"approved","external_reference":"1-42","transaction_amount":5000.0}
                        """, MediaType.APPLICATION_JSON));

        PagoMp pago = client.consultarPago("TOKEN-VENDEDOR", "555");
        assertEquals("approved", pago.status());
        assertEquals("1-42", pago.externalReference());
        assertEquals(0, new BigDecimal("5000.0").compareTo(pago.transactionAmount()));
    }

    @Test
    void reembolsaUnPago() {
        server.expect(requestTo("https://api.mp.test/v1/payments/555/refunds"))
                .andExpect(header("Authorization", "Bearer TOKEN-VENDEDOR"))
                .andExpect(header("X-Idempotency-Key", "refund-555"))
                .andRespond(withSuccess("{\"id\":9001,\"status\":\"approved\"}", MediaType.APPLICATION_JSON));

        client.reembolsarPago("TOKEN-VENDEDOR", "555"); // no lanza
        server.verify();
    }

    @Test
    void respuestaSinCampoRequeridoFallaConMensajeClaro() {
        server.expect(requestTo("https://api.mp.test/v1/payments/555"))
                .andRespond(withSuccess("{\"id\":555,\"status\":\"approved\"}", MediaType.APPLICATION_JSON));
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> client.consultarPago("TOKEN-VENDEDOR", "555"));
        assertTrue(ex.getMessage().contains("transaction_amount"));
    }
}
