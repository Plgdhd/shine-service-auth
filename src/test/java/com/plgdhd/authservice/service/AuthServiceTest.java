package com.plgdhd.authservice.service;

import com.plgdhd.authservice.dto.request.LoginRequest;
import com.plgdhd.authservice.dto.request.RegisterRequest;
import com.plgdhd.authservice.dto.response.TokenResponse;
import com.plgdhd.authservice.exception.InvalidCredentialsException;
import com.plgdhd.authservice.exception.UserAlreadyExistsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    KeycloakUserService keycloakUserService;

    @Mock
    UserEventFacade userEventFacade;

    @Mock
    TokenBlackListService tokenBlackListService;

    @Mock
    RateLimitService rateLimitService;

    @InjectMocks
    AuthService authService;

    private static final String CLIENT_IP = "127.0.0.1";

    @Test
    @DisplayName("register: successful registration")
    void register_success() {
        RegisterRequest request = new RegisterRequest("user@test.com", "test", "test", "VIEWER", "first", "last");
        when(keycloakUserService.createUser(request)).thenReturn("user-uuid-1");

        String userId = authService.register(request);

        assertThat(userId).isEqualTo("user-uuid-1");

        verify(userEventFacade).publishUserRegistered(
                eq("user-uuid-1"),
                eq("user@test.com"),
                eq("test"),
                eq("VIEWER")
        );
    }

    @Test
    @DisplayName("register: duplicate email throws exception")
    void register_duplicateEmail_throwsException() {
        RegisterRequest request = new RegisterRequest("user@test.com", "test", "test", "VIEWER", "first", "last");
        when(keycloakUserService.createUser(request))
                .thenThrow(new UserAlreadyExistsException("Email already taken"));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("Email already taken");

        verifyNoInteractions(userEventFacade);
    }

    @Test
    @DisplayName("login: success")
    void login_success() {
        LoginRequest request = new LoginRequest("user@test.com", "test");
        TokenResponse expectedTokens = TokenResponse.of("access-jwt", "refresh-jwt", 300L, 1800L);

        when(keycloakUserService.login("user@test.com", "test")).thenReturn(expectedTokens);

        TokenResponse result = authService.login(request, CLIENT_IP);

        assertThat(result.accessToken()).isEqualTo("access-jwt");
        assertThat(result.tokenType()).isEqualTo("Bearer");

        verify(rateLimitService).checkLoginRateLimit(CLIENT_IP);
        verify(rateLimitService).resetAttempts(CLIENT_IP);
    }

    @Test
    @DisplayName("login: wrong password records failed attempt")
    void login_wrong_password() {
        LoginRequest request = new LoginRequest("user@test.com", "wrong");

        when(keycloakUserService.login(request.email(), request.password()))
                .thenThrow(new InvalidCredentialsException());

        assertThatThrownBy(() -> authService.login(request, CLIENT_IP))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(rateLimitService).checkLoginRateLimit(CLIENT_IP);
        verify(rateLimitService).recordFailedAttempt(CLIENT_IP);
    }

    @Test
    @DisplayName("logout: blacklists token and revokes refresh token")
    void logout_addsToBlacklistAndRevokesRefresh() {
        Jwt jwt = mockJwt("jti-abc-123", "user-uuid-456", Instant.now().plusSeconds(300));

        authService.logout(jwt, "some-refresh-token");

        verify(tokenBlackListService).addToBlackList(eq("jti-abc-123"), any());
        verify(keycloakUserService).logout("some-refresh-token");
    }

    @Test
    @DisplayName("logout: without refresh token only blacklists")
    void logout_withoutRefreshToken_onlyBlacklist() {
        Jwt jwt = mockJwt("jti-xyz", "user-id", Instant.now().plusSeconds(60));

        authService.logout(jwt, null);

        verify(tokenBlackListService).addToBlackList(eq("jti-xyz"), any());
        verify(keycloakUserService, never()).logout(anyString());
    }

    private Jwt mockJwt(String jti, String subject, Instant expiresAt) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getId()).thenReturn(jti);
        when(jwt.getSubject()).thenReturn(subject);
        when(jwt.getExpiresAt()).thenReturn(expiresAt);
        return jwt;
    }
}
