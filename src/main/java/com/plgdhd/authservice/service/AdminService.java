package com.plgdhd.authservice.service;

import com.plgdhd.authservice.dto.request.BanUserRequest;
import com.plgdhd.authservice.dto.request.ChangeRoleRequest;
import com.plgdhd.authservice.exception.KeycloakException;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import com.plgdhd.authservice.config.KeycloakProperties;

import java.util.List;

@Service
@Slf4j
public class AdminService {

    private final Keycloak keycloakAdminClient;
    private final KeycloakProperties properties;
    private final UserEventFacade userEventFacade;
    private final KeycloakUserService keycloakUserService;

    public AdminService(Keycloak keycloakAdminClient,
                        KeycloakProperties properties,
                        UserEventFacade userEventFacade,
                        KeycloakUserService keycloakUserService) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.properties = properties;
        this.userEventFacade = userEventFacade;
        this.keycloakUserService = keycloakUserService;
    }

    public void banUser(String userId, String adminId, BanUserRequest request) {
        RealmResource realm = keycloakAdminClient.realm(properties.realm());
        UserResource userResource = realm.users().get(userId);

        UserRepresentation user = userResource.toRepresentation();
        user.setEnabled(false);
        userResource.update(user);

        keycloakUserService.invalidateAllUserSessions(userId);

        userEventFacade.publishUserBanned(userId, adminId, request.reason(), request.expiresAt());
        log.info("User {} banned by admin {}", userId, adminId);
    }

    public void changeUserRole(String userId, ChangeRoleRequest request) {
        RealmResource realm = keycloakAdminClient.realm(properties.realm());
        UserResource userResource = realm.users().get(userId);

        List<RoleRepresentation> currentRoles = userResource.roles().realmLevel().listEffective();
        List<String> customRoles = currentRoles.stream()
                .map(RoleRepresentation::getName)
                .filter(r -> !r.startsWith("default-roles-")
                        && !r.equals("offline_access")
                        && !r.equals("uma_authorization"))
                .toList();

        String oldRole = customRoles.isEmpty() ? "NONE" : customRoles.getFirst();

        List<RoleRepresentation> rolesToRemove = currentRoles.stream()
                .filter(r -> customRoles.contains(r.getName()))
                .toList();
        if (!rolesToRemove.isEmpty()) {
            userResource.roles().realmLevel().remove(rolesToRemove);
        }

        try {
            RoleRepresentation newRole = realm.roles().get(request.newRole()).toRepresentation();
            userResource.roles().realmLevel().add(List.of(newRole));
        } catch (Exception e) {
            throw new KeycloakException("Failed to assign role: " + request.newRole(), e);
        }

        userEventFacade.publishUserRoleChanged(userId, oldRole, request.newRole());
        log.info("User {} role changed from {} to {}", userId, oldRole, request.newRole());
    }
}