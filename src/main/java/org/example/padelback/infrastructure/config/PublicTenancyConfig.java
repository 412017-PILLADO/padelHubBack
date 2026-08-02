package org.example.padelback.infrastructure.config;

import java.time.Clock;

import org.example.padelback.infrastructure.tenancy.PublicTenantContextFilter;
import org.example.padelback.infrastructure.web.PublicWriteRateLimitFilter;
import org.example.padelback.infrastructure.web.PublicWriteThrottle;
import org.example.padelback.modules.reservas.infrastructure.web.ClientIpResolver;
import org.example.padelback.modules.tenant.infrastructure.PublicTenantResolver;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PublicTenancyConfig {

    @Bean
    public FilterRegistrationBean<PublicTenantContextFilter> publicTenantContextFilter(PublicTenantResolver resolver) {
        FilterRegistrationBean<PublicTenantContextFilter> registration =
                new FilterRegistrationBean<>(new PublicTenantContextFilter(resolver));
        registration.addUrlPatterns("/public/*");
        registration.setOrder(1);
        return registration;
    }

    /**
     * Rate limit por IP solo sobre los POSTs públicos anónimos de escritura (arrepentimiento y
     * link de seña). El webhook y el callback OAuth quedan afuera a propósito: ver
     * PublicWriteRateLimitFilter.
     */
    @Bean
    public FilterRegistrationBean<PublicWriteRateLimitFilter> publicWriteRateLimitFilter(
            PublicWriteThrottle throttle, ClientIpResolver ipResolver, Clock clock) {
        FilterRegistrationBean<PublicWriteRateLimitFilter> registration =
                new FilterRegistrationBean<>(new PublicWriteRateLimitFilter(throttle, ipResolver, clock));
        registration.addUrlPatterns("/public/arrepentimiento", "/public/pagos/mp/preferencia");
        registration.setOrder(2);
        return registration;
    }
}
