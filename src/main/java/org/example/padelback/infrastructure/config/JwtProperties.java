package org.example.padelback.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("padel.jwt")
public record JwtProperties(String secret, long expiration) {}
