package com.plgdhd.authservice.controller;

import com.plgdhd.authservice.dto.request.LoginRequest;
import com.plgdhd.authservice.dto.request.RefreshTokenRequest;
import com.plgdhd.authservice.dto.request.RegisterRequest;
import com.plgdhd.authservice.dto.response.TokenResponse;
import com.plgdhd.authservice.dto.response.UserInfoResponse;
import com.plgdhd.authservice.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
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
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {

        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest registerRequest) {
        String userId = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(userId);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {

        // Получение Ip для предотвращения брутфорса? －O－
        TokenResponse tokens = authService.login(request);
        return ResponseEntity.status(HttpStatus.OK).body(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request){

        TokenResponse tokens = authService.refresh(request);
        return ResponseEntity.status(HttpStatus.OK).body(tokens);
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getCurrentUser(
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(authService.getCurrentUser(jwt));
    }
}
