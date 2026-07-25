package org.example.padelback.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credenciales de la APLICACIÓN de plataforma en Mercado Pago (una sola para todo el SaaS;
 * cada tenant conecta su cuenta vía OAuth contra esta app). {@code webhookBaseUrl} es la URL
 * pública https del back (vacía en dev: las preferencias se crean sin notification_url).
 */
@ConfigurationProperties("padel.mercadopago")
public record MercadoPagoProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String webhookBaseUrl,
        String apiBase,
        String authBase) {

    public boolean configurado() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }
}
