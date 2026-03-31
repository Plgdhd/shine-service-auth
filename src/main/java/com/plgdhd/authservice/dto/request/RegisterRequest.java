package com.plgdhd.authservice.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

public record RegisterRequest(

        @NotBlank(message = "Email обязателен")
        @Email(message = "Некорректный формат email")
        String email,

        @NotBlank(message = "Username обязателен")
        @Size(min = 3, max = 30, message = "Username: от 3 до 30 символов")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$",
                message = "Username: только буквы, цифры, _ и -")
        String username,

        @NotBlank(message = "Пароль обязателен")
        @Size(min = 8, message = "Пароль минимум 8 символов")
        String password,

        // VIEWER — дефолтная роль. Стримером становятся через отдельный процесс
        @Pattern(regexp = "^(VIEWER|STREAMER)$",
                message = "Роль должна быть VIEWER или STREAMER")
        String role,

        @JsonProperty("first_name")
        @Schema(description = "Имя пользователя", example = "Василиййййййй")
        String firstName,

        @JsonProperty("last_name")
        @Schema(description = "Фамилия пользователя", example = "Пупкин-Залупкин")
        String lastName

) {

    public RegisterRequest {
        if (email != null) email = email.toLowerCase().trim();
        if (username != null) username = username.trim();
        if (role == null) role = "VIEWER";
    }
}