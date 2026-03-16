package com.plgdhd.authservice.service;

import com.plgdhd.authservice.dto.request.LoginRequest;
import com.plgdhd.authservice.dto.request.RefreshTokenRequest;
import com.plgdhd.authservice.dto.request.RegisterRequest;
import com.plgdhd.authservice.dto.response.TokenResponse;
import com.plgdhd.authservice.dto.response.UserInfoResponse;
import com.plgdhd.authservice.exception.InvalidCredentialsException;
import com.plgdhd.authservice.infrastructure.publisher.UserBannedPublisher;
import com.plgdhd.authservice.infrastructure.publisher.UserRegisteredPublisher;
import com.plgdhd.authservice.infrastructure.publisher.UserRoleChangedPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AuthService {

    private final KeycloakUserService keycloakUserService;
    private final TokenBlackListService tokenBlackListService;
    private final RateLimitService rateLimitService;
    private final UserEventFacade userEventFacade;

    @Autowired
    public AuthService(KeycloakUserService keycloakUserService,
                       TokenBlackListService tokenBlackListService,
                       RateLimitService rateLimitService,
                       UserEventFacade userEventFacade){
        this.keycloakUserService = keycloakUserService;
        this.tokenBlackListService = tokenBlackListService;
        this.rateLimitService = rateLimitService;
        this.userEventFacade = userEventFacade;
    }

    public String register(RegisterRequest request){

        String userId = keycloakUserService.createUser(request);

        userEventFacade.publishUserRegistered(userId, request.email(), request.username(), request.role());

        log.info("Пользователь зарегестрирован успешно: userId={}, role={}", userId, request.role());
        return userId;
    }

    public TokenResponse login(LoginRequest request){

//        rateLimitService.checkLoginRateLimit(clientIp);

        try{
            TokenResponse tokens = keycloakUserService.login(request.emailOrUsername(), request.password());

//            rateLimitService.resetAttempts(clientIp);

            log.info("Успешный вход: user={}", request.emailOrUsername());
            return tokens;
        }
        catch (InvalidCredentialsException ex){

//            rateLimitService.recordFailedAttempt(clientIp);

            log.warn("Неудачная попытка входа: user={}",  request.emailOrUsername());
            throw  ex;
        }
    }

    public TokenResponse refresh(RefreshTokenRequest request){
        return keycloakUserService.refreshToken(request.refreshToken());
    }

    public void logout(Jwt jwt, String refreshToken){
        String jti = jwt.getId();
        Instant expiresAt = jwt.getExpiresAt();

        if(jti != null && expiresAt != null){
            tokenBlackListService.addToBlackList(jti, expiresAt);
            log.debug("Access-токен заблокирован: jti={}", jti);
        }

        if(refreshToken != null && !refreshToken.isBlank()){
            keycloakUserService.logout(refreshToken);
        }

        log.info("Выполнен выход из аккаунта: userId={}", jwt.getSubject());
    }


    public UserInfoResponse getCurrentUser(Jwt jwt) {
        return new UserInfoResponse(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("preferred_username"),
                extractRoles(jwt)
        );
    }

    private List<String> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
            return roles.stream()
                    .map(String::valueOf)
                    .toList();
        }

        return List.of();
    }
}
