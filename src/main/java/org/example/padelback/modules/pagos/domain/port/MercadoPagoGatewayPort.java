package org.example.padelback.modules.pagos.domain.port;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.example.padelback.modules.pagos.domain.model.PagoMp;
import org.example.padelback.modules.pagos.domain.model.PreferenciaSena;
import org.example.padelback.modules.pagos.domain.model.TokensMp;

/** Llamadas salientes a la API de Mercado Pago. */
public interface MercadoPagoGatewayPort {

    TokensMp intercambiarCode(String code);

    TokensMp refrescar(String refreshToken);

    /**
     * Crea la preferencia de Checkout Pro EN NOMBRE del tenant (con su access token).
     * @param notificationUrl null = sin webhook (dev); @param expiraEn vencimiento de la
     * preferencia = expira_en de la reserva (hora de negocio); @param backUrl retorno del checkout.
     */
    PreferenciaSena crearPreferencia(String accessToken, String titulo, BigDecimal monto,
                                     String externalReference, String notificationUrl,
                                     LocalDateTime expiraEn, String backUrl);

    PagoMp consultarPago(String accessToken, String paymentId);

    /** Reembolso TOTAL del pago (devolución de la seña) en la cuenta MP del tenant. */
    void reembolsarPago(String accessToken, String paymentId);
}
