package org.example.padelback.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * {@link RestClient.Builder} para los adapters HTTP salientes (hoy: {@code MercadoPagoClient}).
 * Con {@code spring-boot-starter-webmvc} (Boot 4) el módulo {@code spring-boot-restclient} y su
 * auto-configuración NO están en el classpath, así que el builder hay que declararlo a mano.
 * Prototype como el de Boot: cada adapter recibe su propio builder mutable.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    @org.springframework.context.annotation.Scope("prototype")
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
