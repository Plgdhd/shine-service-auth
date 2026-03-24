package com.plgdhd.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    //TODO переделать под Email
    @NotBlank(message = "Email обязателен")
    String email,

    @NotBlank(message = "Пароль обязателен")
    String password
) {}

