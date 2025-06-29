package com.lagab.eventz.app.auth.dto.apikey;

import com.lagab.eventz.app.auth.entity.ApiKey;

public record ApiKeyAuthenticationPrincipal(ApiKey apiKey) {

    public String getName() {
        return apiKey.getName();
    }

    public String getClientType() {
        return apiKey.getClientType();
    }

    public boolean isApiKeyAuthentication() {
        return true;
    }
}
