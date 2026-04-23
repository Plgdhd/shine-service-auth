package com.plgdhd.authservice.integration;

import com.plgdhd.authservice.dto.request.LoginRequest;
import com.plgdhd.authservice.dto.request.RefreshTokenRequest;
import com.plgdhd.authservice.dto.request.RegisterRequest;
import com.plgdhd.authservice.dto.response.TokenResponse;
import com.plgdhd.authservice.exception.InvalidCredentialsException;
import com.plgdhd.authservice.exception.RateLimitException;
import com.plgdhd.authservice.exception.UserAlreadyExistsException;
import com.plgdhd.authservice.infrastructure.EventSender;
import com.plgdhd.authservice.service.KeycloakUserService;
import com.plgdhd.authservice.service.RateLimitService;
import com.plgdhd.authservice.service.TokenBlackListService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private KeycloakUserService keycloakUserService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @MockitoBean
    private TokenBlackListService tokenBlackListService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private Keycloak keycloakAdminClient;

    @MockitoBean
    private EventSender eventSender;

    @MockitoBean
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @Nested
    @DisplayName("POST /auth/register")
    class RegisterTests {

        @Test
        @DisplayName("201 on successful registration")
        void register_success() throws Exception {
            RegisterRequest request = new RegisterRequest(
                    "user@test.com", "testuser", "Password123", "VIEWER", "John", "Doe");

            when(keycloakUserService.createUser(any(RegisterRequest.class))).thenReturn("user-uuid-1");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(content().string("user-uuid-1"));
        }

        @Test
        @DisplayName("409 when user already exists")
        void register_conflict() throws Exception {
            RegisterRequest request = new RegisterRequest(
                    "user@test.com", "testuser", "Password123", "VIEWER", "John", "Doe");

            when(keycloakUserService.createUser(any(RegisterRequest.class)))
                    .thenThrow(new UserAlreadyExistsException("User with this username/email already exists"));

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }

        @Test
        @DisplayName("400 on invalid request body")
        void register_validationError() throws Exception {
            String invalidJson = """
                    {"email": "", "username": "", "password": "short"}
                    """;

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 when email is missing")
        void register_missingEmail() throws Exception {
            String json = """
                    {"username": "testuser", "password": "Password123"}
                    """;

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 when username is too short")
        void register_usernameTooShort() throws Exception {
            String json = """
                    {"email": "test@test.com", "username": "ab", "password": "Password123"}
                    """;

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 when invalid role specified")
        void register_invalidRole() throws Exception {
            String json = """
                    {"email": "test@test.com", "username": "testuser", "password": "Password123", "role": "HACKER"}
                    """;

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("201 with default role when role is omitted")
        void register_defaultRole() throws Exception {
            String json = """
                    {"email": "test@test.com", "username": "testuser", "password": "Password123"}
                    """;

            when(keycloakUserService.createUser(any(RegisterRequest.class))).thenReturn("user-uuid-2");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("POST /auth/login")
    class LoginTests {

        @Test
        @DisplayName("200 on successful login")
        void login_success() throws Exception {
            LoginRequest request = new LoginRequest("user@test.com", "Password123");
            TokenResponse tokens = TokenResponse.of("access-token", "refresh-token", 300L, 1800L);

            when(keycloakUserService.login("user@test.com", "Password123")).thenReturn(tokens);

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.access_token").value("access-token"))
                    .andExpect(jsonPath("$.refresh_token").value("refresh-token"))
                    .andExpect(jsonPath("$.token_type").value("Bearer"))
                    .andExpect(jsonPath("$.expires_in").value(300));

            verify(rateLimitService).checkLoginRateLimit(anyString());
            verify(rateLimitService).resetAttempts(anyString());
        }

        @Test
        @DisplayName("401 on invalid credentials")
        void login_invalidCredentials() throws Exception {
            LoginRequest request = new LoginRequest("user@test.com", "WrongPassword");

            when(keycloakUserService.login("user@test.com", "WrongPassword"))
                    .thenThrow(new InvalidCredentialsException());

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());

            verify(rateLimitService).recordFailedAttempt(anyString());
        }

        @Test
        @DisplayName("429 when rate limited")
        void login_rateLimited() throws Exception {
            LoginRequest request = new LoginRequest("user@test.com", "Password123");

            doThrow(new RateLimitException(900L)).when(rateLimitService).checkLoginRateLimit(anyString());

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(header().string("Retry-After", "900"));
        }

        @Test
        @DisplayName("400 when email is blank")
        void login_blankEmail() throws Exception {
            String json = """
                    {"email": "", "password": "Password123"}
                    """;

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /auth/refresh")
    class RefreshTests {

        @Test
        @DisplayName("200 on successful token refresh")
        void refresh_success() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
            TokenResponse tokens = TokenResponse.of("new-access", "new-refresh", 300L, 1800L);

            when(keycloakUserService.refreshToken("valid-refresh-token")).thenReturn(tokens);

            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.access_token").value("new-access"))
                    .andExpect(jsonPath("$.refresh_token").value("new-refresh"));
        }

        @Test
        @DisplayName("401 on invalid refresh token")
        void refresh_invalidToken() throws Exception {
            RefreshTokenRequest request = new RefreshTokenRequest("expired-token");

            when(keycloakUserService.refreshToken("expired-token"))
                    .thenThrow(new InvalidCredentialsException());

            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("400 when refresh token is blank")
        void refresh_blankToken() throws Exception {
            String json = """
                    {"refreshToken": ""}
                    """;

            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("204 on successful logout")
        void logout_success() throws Exception {
            String json = """
                    {"refreshToken": "some-refresh-token"}
                    """;

            mockMvc.perform(post("/auth/logout")
                            .with(jwt().jwt(builder -> builder
                                    .claim("sub", "user-id-123")
                                    .claim("jti", "jti-abc")
                                    .claim("email", "user@test.com")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("401 when not authenticated")
        void logout_unauthenticated() throws Exception {
            String json = """
                    {"refreshToken": "some-refresh-token"}
                    """;

            mockMvc.perform(post("/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /auth/me")
    class GetCurrentUserTests {

        @Test
        @DisplayName("200 with user info for authenticated user")
        void getCurrentUser_success() throws Exception {
            mockMvc.perform(get("/auth/me")
                            .with(jwt().jwt(builder -> builder
                                    .claim("sub", "user-id-123")
                                    .claim("email", "user@test.com")
                                    .claim("preferred_username", "testuser")
                                    .claim("realm_access", java.util.Map.of(
                                            "roles", java.util.List.of("VIEWER"))))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value("user-id-123"))
                    .andExpect(jsonPath("$.email").value("user@test.com"))
                    .andExpect(jsonPath("$.username").value("testuser"))
                    .andExpect(jsonPath("$.roles[0]").value("VIEWER"));
        }

        @Test
        @DisplayName("401 when not authenticated")
        void getCurrentUser_unauthenticated() throws Exception {
            mockMvc.perform(get("/auth/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Admin endpoints security")
    class AdminSecurityTests {

        @Test
        @DisplayName("401 for unauthenticated admin request")
        void adminEndpoint_unauthenticated() throws Exception {
            mockMvc.perform(post("/admin/users/some-user-id/ban")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"reason": "spam"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 for non-admin user")
        void adminEndpoint_forbidden() throws Exception {
            mockMvc.perform(post("/admin/users/some-user-id/ban")
                            .with(jwt().jwt(builder -> builder
                                    .claim("sub", "user-id")
                                    .claim("realm_access", java.util.Map.of(
                                            "roles", java.util.List.of("VIEWER")))))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"reason": "spam"}
                                    """))
                    .andExpect(status().isForbidden());
        }
    }
}
