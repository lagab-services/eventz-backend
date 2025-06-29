package com.lagab.eventz.app.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthResponse(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("token_type")
        String tokenType,

        @JsonProperty("expires_in")
        Long expiresIn,

        UserResponse user
) {
}
