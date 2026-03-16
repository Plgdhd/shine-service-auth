package com.plgdhd.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "Refresh токен обязателен")
        String refreshToken
) {}
