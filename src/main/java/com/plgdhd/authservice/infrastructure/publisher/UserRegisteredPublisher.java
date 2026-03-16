package com.plgdhd.authservice.infrastructure.publisher;

import com.plgdhd.auth.event.proto.UserRegisteredEvent;
import com.plgdhd.authservice.infrastructure.EventSender;
import org.springframework.kafka.support.SendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class UserRegisteredPublisher {

    private final EventSender eventSender;

    @Value("${app.kafka.topics.user-registered}")
    private String userRegisteredTopic;

    @Autowired
    public UserRegisteredPublisher(EventSender eventSender) {
        this.eventSender = eventSender;
    }

    public void publishUserRegistered(String userId, String email, String username, String role ) {

        UserRegisteredEvent event = UserRegisteredEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setUserId(userId)
                .setEmail(email)
                .setUsername(username)
                .setRole(role)
                .setRegisteredAt(Instant.now().toEpochMilli())
                .build();

        eventSender.send(userRegisteredTopic, userId, event.toByteArray());


    }
}
