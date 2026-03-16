package com.plgdhd.authservice.service;

import com.plgdhd.auth.event.proto.UserRegisteredEvent;
import com.plgdhd.authservice.infrastructure.publisher.UserBannedPublisher;
import com.plgdhd.authservice.infrastructure.publisher.UserRegisteredPublisher;
import com.plgdhd.authservice.infrastructure.publisher.UserRoleChangedPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class UserEventFacade {

    private final UserRegisteredPublisher userRegisteredPublisher;
    private final UserBannedPublisher userBannedPublisher;
    private final UserRoleChangedPublisher userRoleChangedPublisher;

    @Autowired
    public UserEventFacade(UserRegisteredPublisher userRegisteredPublisher,
                           UserBannedPublisher userBannedPublisher,
                           UserRoleChangedPublisher userRoleChangedPublisher){
        this.userRegisteredPublisher = userRegisteredPublisher;
        this.userBannedPublisher = userBannedPublisher;
        this.userRoleChangedPublisher = userRoleChangedPublisher;
    }

    public void publishUserRegistered(String userId, String email, String username, String role){
        userRegisteredPublisher.publishUserRegistered(userId, email, username, role);
    }

    public void publishUserBanned(String userId, String adminId, String reason, Instant expiresAt){
        userBannedPublisher.publishUserBanned(userId, adminId, reason, expiresAt);
    }

    public void publishUserRoleChanged(String userId, String oldRole, String newRole){
        userRoleChangedPublisher.publishUserRoleChangedEvent(userId, oldRole, newRole);
    }
}
