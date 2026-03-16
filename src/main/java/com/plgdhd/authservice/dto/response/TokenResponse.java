package com.plgdhd.authservice.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenResponse(

        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("token_type")
        String tokenType,

        @JsonProperty("expires_in")
        long expiresIn,

        @JsonProperty("refresh_expires_in")
        long refreshExpiresIn
) {
    public static TokenResponse of(String accessToken, String refreshToken,
                                   long expiresIn, long refreshExpiresIn) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresIn, refreshExpiresIn);
    }
}
