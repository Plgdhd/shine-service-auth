package com.plgdhd.authservice.filter;

import com.plgdhd.authservice.dto.exception.ApiErrorResponse;
import com.plgdhd.authservice.service.TokenBlackListService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Slf4j
@Component
public class TokenBlackListFilter extends OncePerRequestFilter {

    private final TokenBlackListService tokenBlackListService;
    private final JwtDecoder jwtDecoder;
    private final ObjectMapper objectMapper;

    @Autowired
    public TokenBlackListFilter(TokenBlackListService tokenBlackListService, JwtDecoder jwtDecoder, ObjectMapper objectMapper) {
        this.tokenBlackListService = tokenBlackListService;
        this.jwtDecoder = jwtDecoder;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try{
            Jwt jwt = jwtDecoder.decode(token);
            String jti = jwt.getId();

            if(jti != null && tokenBlackListService.isBlackListed(jti)){
                log.warn("Отозванный токен: jti{}, path{}, user{}", jti, request.getRequestURI(), jwt.getSubject());
                sendUnauthorized(response, request.getRequestURI(), "Токен отозван");
                return;
            }
        }
        catch (JwtException ex){
            filterChain.doFilter(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response, String path, String message)
            throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiErrorResponse error = ApiErrorResponse.of(401, "Unauthorized", message, path);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
