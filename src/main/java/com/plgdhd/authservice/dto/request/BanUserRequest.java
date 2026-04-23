package com.plgdhd.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record BanUserRequest(
        @NotBlank(message = "Reason is required")
        String reason,
        Instant expiresAt
) {}