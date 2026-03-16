package com.plgdhd.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keycloak")
public record KeycloakProperties(
        String authServerUrl,
        String realm,
        String clientId,
        String clientSecret,
        String defaultUserRole
) {}