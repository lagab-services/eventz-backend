package com.lagab.eventz.app.infrastructure.security;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.lagab.eventz.app.domain.auth.dto.apikey.ApiKeyAuthenticationPrincipal;
import com.lagab.eventz.app.domain.auth.service.ApiKeyService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String apiKey = extractApiKey(request);

        try {
            if (apiKey != null) {
                authenticateWithApiKey(apiKey);
            }

        } catch (Exception e) {
            log.error("Authentication error", e);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateWithApiKey(String keyValue) {
        apiKeyService.validateApiKey(keyValue).ifPresent(apiKey -> {
            // Create a custom principal for API keys
            ApiKeyAuthenticationPrincipal principal = new ApiKeyAuthenticationPrincipal(apiKey);

            Collection<GrantedAuthority> authorities = apiKey.getRoles().stream()
                                                             .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                                                             .collect(Collectors.toList());

            PreAuthenticatedAuthenticationToken auth = new PreAuthenticatedAuthenticationToken(principal, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Successful authentication with API key: {}", apiKey.getName());
        });
    }

    private String extractApiKey(HttpServletRequest request) {
        // X-API-Key header
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null) {
            return apiKey;
        }

        // Query parameter
        apiKey = request.getParameter("api_key");
        if (apiKey != null) {
            return apiKey;
        }

        // Authorization: ApiKey xxx
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("ApiKey ")) {
            return authHeader.substring(7);
        }

        return null;
    }

}
