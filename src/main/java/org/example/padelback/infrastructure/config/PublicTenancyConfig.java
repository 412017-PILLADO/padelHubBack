package org.example.padelback.infrastructure.config;

import org.example.padelback.infrastructure.tenancy.PublicTenantContextFilter;
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
}
