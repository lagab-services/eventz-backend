package com.lagab.eventz.app.interfaces.web.auth;

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

import com.lagab.eventz.app.domain.auth.model.ApiKey;
import com.lagab.eventz.app.domain.auth.service.ApiKeyService;
import com.lagab.eventz.app.domain.user.model.Role;
import com.lagab.eventz.app.interfaces.web.auth.dto.ApiKeyResponse;
import com.lagab.eventz.app.interfaces.web.auth.dto.CreateApiKeyRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "API Key Management", description = "Endpoints for managing API keys. Requires ADMIN role.")
@SecurityRequirement(name = "bearerAuth")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    /**
     * Creates a new API key with the specified parameters.
     *
     * @param request The API key creation request containing name, client type, roles, and expiration date
     * @return ResponseEntity containing the created API key details (including the secret key)
     */
    @PostMapping
    @Operation(
            summary = "Create a new API key",
            description = "Creates a new API key with specified name, client type, roles, and expiration date. Returns the API key with its secret value."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "API key created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiKeyResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - ADMIN role required",
                    content = @Content
            )
    })
    public ResponseEntity<ApiKeyResponse> createApiKey(
            @Parameter(description = "API key creation request", required = true)
            @RequestBody @Valid CreateApiKeyRequest request) {
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
    @Operation(
            summary = "Revoke an API key",
            description = "Revokes an existing API key by its ID. The API key will no longer be valid for authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "API key revoked successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - ADMIN role required",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "API key not found",
                    content = @Content
            )
    })
    public ResponseEntity<Void> revokeApiKey(
            @Parameter(description = "ID of the API key to revoke", required = true, example = "1")
            @PathVariable Long id) {
        apiKeyService.revokeApiKey(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get API key by ID",
            description = "Retrieves a specific API key by its ID. The secret key value is not included in the response."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "API key found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiKeyResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - ADMIN role required",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "API key not found",
                    content = @Content
            )
    })
    public ResponseEntity<ApiKeyResponse> getApiKey(
            @Parameter(description = "ID of the API key to retrieve", required = true, example = "1")
            @PathVariable Long id) {
        ApiKeyResponse apiKeyResponse = apiKeyService.getApikeyById(id);
        return ResponseEntity.ok(apiKeyResponse);
    }

    /**
     * Retrieves all API keys (without exposing the secret key values).
     *
     * @return ResponseEntity containing a list of API key responses (without secret keys)
     */
    @GetMapping
    @Operation(
            summary = "Get all API keys",
            description = "Retrieves all API keys in the system. Secret key values are not included in the response for security reasons."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of API keys retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiKeyResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - ADMIN role required",
                    content = @Content
            )
    })
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
    @Operation(
            summary = "Get API keys by client type",
            description = "Retrieves API keys filtered by client type. Secret key values are not included in the response for security reasons."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of API keys for the specified client type retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiKeyResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - ADMIN role required",
                    content = @Content
            )
    })
    public ResponseEntity<List<ApiKeyResponse>> getApiKeysByType(
            @Parameter(description = "Client type to filter API keys", required = true, example = "WEB_CLIENT")
            @PathVariable String clientType) {
        List<ApiKeyResponse> responses = apiKeyService.getApiKeysByType(clientType).stream()
                                                      .map(ApiKeyResponse::withoutKey)
                                                      .toList();

        return ResponseEntity.ok(responses);
    }
}
