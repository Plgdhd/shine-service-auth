package com.plgdhd.authservice.infrastructure.publisher;

import com.plgdhd.auth.event.proto.UserRoleChangedEvent;
import com.plgdhd.authservice.infrastructure.EventSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class UserRoleChangedPublisher {

    private final EventSender eventSender;

    @Value("${app.kafka.topics.user-role-changed}")
    private String userRoleChangedTopic;

    @Autowired
    public UserRoleChangedPublisher(EventSender eventSender) {
        this.eventSender = eventSender;
    }

    public void publishUserRoleChangedEvent(String userId, String oldRole, String newRole) {

        UserRoleChangedEvent event = UserRoleChangedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setUserId(userId)
                .setOldRole(oldRole)
                .setNewRole(newRole)
                .setChangedAt(Instant.now().toEpochMilli())
                .build();

        eventSender.send(userRoleChangedTopic, userId, event.toByteArray());
    }
}
