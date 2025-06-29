package com.lagab.eventz.app.auth.controller;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lagab.eventz.app.auth.dto.apikey.ApiKeyResponse;
import com.lagab.eventz.app.auth.dto.apikey.CreateApiKeyRequest;
import com.lagab.eventz.app.auth.entity.ApiKey;
import com.lagab.eventz.app.auth.service.ApiKeyService;
import com.lagab.eventz.app.user.entity.Role;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for managing API keys.
 * Provides endpoints for creating, revoking, and retrieving API keys.
 * All endpoints require ADMIN role access.
 */
@RestController
@RequestMapping("/api/admin/apikeys")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    /**
     * Creates a new API key with the specified parameters.
     *
     * @param request The API key creation request containing name, client type, roles, and expiration date
     * @return ResponseEntity containing the created API key details (including the secret key)
     */
    @PostMapping
    public ResponseEntity<ApiKeyResponse> createApiKey(@RequestBody @Valid CreateApiKeyRequest request) {
        Set<Role> roles = request.roles().stream()
                                 .map(Role::valueOf)
                                 .collect(Collectors.toSet());

        ApiKey apiKey = apiKeyService.createApiKey(
                request.name(),
                request.clientType(),
                roles,
                request.expiresAt()
        );

        log.info("API key created: {} for client type: {}", request.name(), request.clientType());
        return ResponseEntity.ok(ApiKeyResponse.withKey(apiKey));
    }

    /**
     * Revokes an existing API key by its ID.
     *
     * @param id The ID of the API key to revoke
     * @return ResponseEntity with no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeApiKey(@PathVariable Long id) {
        apiKeyService.revokeApiKey(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiKeyResponse> getApiKey(@PathVariable Long id) {
        ApiKeyResponse apiKeyResponse = apiKeyService.getApikeyById(id);
        return ResponseEntity.ok(apiKeyResponse);
    }

    /**
     * Retrieves all API keys (without exposing the secret key values).
     *
     * @return ResponseEntity containing a list of API key responses (without secret keys)
     */
    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> getAllApiKeys() {
        List<ApiKeyResponse> responses = apiKeyService.getApiKeysByType(null).stream()
                                                      .map(ApiKeyResponse::withoutKey)
                                                      .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * Retrieves API keys filtered by client type (without exposing the secret key values).
     *
     * @param clientType The client type to filter by
     * @return ResponseEntity containing a list of matching API key responses (without secret keys)
     */
    @GetMapping("/type/{clientType}")
    public ResponseEntity<List<ApiKeyResponse>> getApiKeysByType(@PathVariable String clientType) {
        List<ApiKeyResponse> responses = apiKeyService.getApiKeysByType(clientType).stream()
                                                      .map(ApiKeyResponse::withoutKey)
                                                      .toList();

        return ResponseEntity.ok(responses);
    }
}
