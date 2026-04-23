package com.plgdhd.authservice.service;

import com.plgdhd.authservice.config.KeycloakProperties;
import com.plgdhd.authservice.dto.request.RegisterRequest;
import com.plgdhd.authservice.dto.response.TokenResponse;
import com.plgdhd.authservice.exception.InvalidCredentialsException;
import com.plgdhd.authservice.exception.KeycloakException;
import com.plgdhd.authservice.exception.UserAlreadyExistsException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class KeycloakUserService {

    private final Keycloak keycloakAdminClient;
    private final KeycloakProperties properties;
    private final WebClient webClient;

    public KeycloakUserService(Keycloak keycloakAdminClient,
                               KeycloakProperties properties,
                               WebClient webClient) {
        this.properties = properties;
        this.keycloakAdminClient = keycloakAdminClient;
        this.webClient = webClient;
    }

    public String createUser(RegisterRequest request) {
        RealmResource realmResource = keycloakAdminClient.realm(properties.realm());

        UserRepresentation user = buildUserRepresentation(request);
        Response response = realmResource.users().create(user);

        log.debug("Create user in keycloak: status={}", response.getStatus());

        if (response.getStatus() == HttpStatus.CONFLICT.value()) {
            throw new UserAlreadyExistsException("User with this username/email already exists");
        }

        if (response.getStatus() != HttpStatus.CREATED.value()) {
            String body = response.readEntity(String.class);
            throw new KeycloakException("Failed to create user, status=" + response.getStatus()
                    + " body=" + body);
        }

        String location = response.getHeaderString("Location");
        String userId = location.substring(location.lastIndexOf("/") + 1);

        log.info("User created in keycloak: userId={}, username={}", userId, request.username());

        assignRealmRole(realmResource, userId, properties.defaultUserRole());

        return userId;
    }

    private UserRepresentation buildUserRepresentation(RegisterRequest request) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.password());
        credential.setTemporary(false);

        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setFirstName(request.firstName() != null ? request.firstName() : "");
        user.setLastName(request.lastName() != null ? request.lastName() : "");
        user.setEnabled(true);
        user.setEmailVerified(true); // TODO добавить flow подтеверждения email
        user.setCredentials(List.of(credential));
        user.setAttributes(Map.of("display_name", List.of(request.username())));

        return user;
    }

    // роли в realm_access.roles
    private void assignRealmRole(RealmResource realmResource, String userId, String roleName){
        try{
            RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
            realmResource.users().get(userId).roles().realmLevel().add(List.of(role));
            log.debug("Role {} assigned to user {}", roleName, userId);
        } catch (Exception e) {
            throw new KeycloakException("Failed to assign role: " + roleName, e);
        }
    }

    public TokenResponse login(String email, String password) {
        String tokenUrl = properties.authServerUrl() + "/realms/" + properties.realm()
                + "/protocol/openid-connect/token";

        try {
            TokenResponse response = webClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("grant_type", "password")
                            .with("client_id", properties.clientId())
                            .with("client_secret", properties.clientSecret())
                            .with("username", email)
                            .with("password", password)
                            .with("scope", "openid")
                    )
                    .retrieve()
                    .bodyToMono(TokenResponse.class)
                    .block();

            if (response == null) {
                throw new KeycloakException("Empty response from Keycloak during login");
            }

            return response;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new InvalidCredentialsException();
            }
            throw new KeycloakException("Login error: " + e.getMessage(), e);
        }
    }

    public TokenResponse refreshToken(String refreshToken) {
        String tokenUrl = properties.authServerUrl() + "/realms/" + properties.realm()
                + "/protocol/openid-connect/token";

        try {
            TokenResponse response = webClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("grant_type", "refresh_token")
                            .with("client_id", properties.clientId())
                            .with("client_secret", properties.clientSecret())
                            .with("refresh_token", refreshToken)
                    )
                    .retrieve()
                    .bodyToMono(TokenResponse.class)
                    .block();

            if (response == null) {
                throw new KeycloakException("Empty response from Keycloak during token refresh");
            }

            return response;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new InvalidCredentialsException();
            }
            throw new KeycloakException("Token refresh error: " + e.getMessage(), e);
        }
    }

    public void logout(String refreshToken) {
        String tokenUrl = properties.authServerUrl() + "/realms/" + properties.realm()
                + "/protocol/openid-connect/logout";

        try {
            webClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("client_id", properties.clientId())
                            .with("client_secret", properties.clientSecret())
                            .with("refresh_token", refreshToken))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.warn("Logout error, refresh token may be invalid: {}", e.getMessage());
        }
    }

    public Optional<UserRepresentation> findUserById(String userId) {
        try {
            return Optional.ofNullable(
                    keycloakAdminClient.realm(properties.realm()).users().get(userId).toRepresentation()
            );
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void invalidateAllUserSessions(String userId) {
        keycloakAdminClient.realm(properties.realm()).users().get(userId).logout();
        log.info("All sessions invalidated for user {}", userId);
    }
}