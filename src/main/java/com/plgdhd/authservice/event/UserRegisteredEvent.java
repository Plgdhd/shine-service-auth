package com.plgdhd.authservice.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import org.springframework.context.annotation.Scope;

import java.time.Instant;
import java.util.UUID;


@Builder
public record UserRegisteredEvent (

        @JsonProperty("event_id")
        String eventId,

        @JsonProperty("user_id")
        String userId,

        @JsonProperty("email")
        String email,

        @JsonProperty("username")
        String username,

        @JsonProperty("occurred_at")
        Instant occurredAt
){
    public static UserRegisteredEvent of(String userId, String email, String username) {
        return UserRegisteredEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(userId)
                .email(email)
                .username(username)
                .occurredAt(Instant.now())
                .build();
    }
}
