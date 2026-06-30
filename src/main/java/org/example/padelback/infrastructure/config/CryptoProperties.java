package org.example.padelback.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("padel.crypto")
public record CryptoProperties(String key) {}
