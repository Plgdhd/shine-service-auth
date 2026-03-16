package com.plgdhd.authservice.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableConfigurationProperties(KeycloakProperties.class)
public class KeycloakAdminConfig {

    @Bean
    public Keycloak keycloakAdminClient(KeycloakProperties keycloakProperties) {
        return KeycloakBuilder.builder()
                .serverUrl(keycloakProperties.authServerUrl())
                .realm(keycloakProperties.realm())
                .clientId(keycloakProperties.clientId())
                .clientSecret(keycloakProperties.clientSecret())
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .build();
    }

    @Bean
    public WebClient webClient(){
        return WebClient.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(256 * 1024))
                .build();
    }
}
