package com.plgdhd.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangeRoleRequest(
        @NotBlank(message = "New role is required")
        @Pattern(regexp = "^(VIEWER|STREAMER|MODERATOR|ADMIN)$",
                message = "Role must be VIEWER, STREAMER, MODERATOR or ADMIN")
        String newRole
) {}