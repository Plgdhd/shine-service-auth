package com.plgdhd.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    //TODO переделать под Email
    @NotBlank(message = "Email или Username обязателен")
    String emailOrUsername,

    @NotBlank(message = "Пароль обязателен")
    String password
) {}

