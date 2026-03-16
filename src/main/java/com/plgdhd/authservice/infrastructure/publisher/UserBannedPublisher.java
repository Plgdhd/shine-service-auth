package com.plgdhd.authservice.infrastructure.publisher;

import com.plgdhd.auth.event.proto.UserBannedEvent;
import com.plgdhd.authservice.infrastructure.EventSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@Slf4j
public class UserBannedPublisher {

    private final EventSender eventSender;

    @Value("${app.kafka.topics.user-banned}")
    private String userBannedTopic;

    @Autowired
    public  UserBannedPublisher(EventSender eventSender) {
        this.eventSender = eventSender;
    }

    public void publishUserBanned(String userId, String adminId, String reason, Instant expiresAt){
        UserBannedEvent event = UserBannedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setUserId(userId)
                .setBannedByAdminId(adminId)
                .setReason(reason)
                .setBannedAt(Instant.now().toEpochMilli())
                .setBanExpiresAt(expiresAt != null ? expiresAt.toEpochMilli() : 0L)
                .build();

        eventSender.send(userBannedTopic, userId, event.toByteArray());
    }
}
