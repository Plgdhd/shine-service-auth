package com.plgdhd.authservice.controller;

import com.plgdhd.authservice.dto.request.LoginRequest;
import com.plgdhd.authservice.dto.request.RefreshTokenRequest;
import com.plgdhd.authservice.dto.request.RegisterRequest;
import com.plgdhd.authservice.dto.response.TokenResponse;
import com.plgdhd.authservice.dto.response.UserInfoResponse;
import com.plgdhd.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/auth")
@Tag(name = "Контроллер авторизации")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {

        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Регистрация пользователя")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest registerRequest) {
        String userId = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(userId);
    }

    @PostMapping("/login")
    @Operation(summary = "Вход пользователя")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {

        // Получение Ip для предотвращения брутфорса? －O－
        TokenResponse tokens = authService.login(request);
        return ResponseEntity.status(HttpStatus.OK).body(tokens);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Ого рефреш токен")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request){

        TokenResponse tokens = authService.refresh(request);
        return ResponseEntity.status(HttpStatus.OK).body(tokens);
    }

    @GetMapping("/me")
    @Operation(summary = "Получение текущего пользователя по токену")
    public ResponseEntity<UserInfoResponse> getCurrentUser(
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(authService.getCurrentUser(jwt));
    }
}
