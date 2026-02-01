package com.lagab.eventz.app.interfaces.web.org;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lagab.eventz.app.common.exception.UnauthorizedException;
import com.lagab.eventz.app.domain.org.dto.OrganizationDto;
import com.lagab.eventz.app.domain.org.service.OrganizationService;
import com.lagab.eventz.app.domain.user.model.User;
import com.lagab.eventz.app.interfaces.web.org.dto.CreateOrganizationRequestDto;
import com.lagab.eventz.app.interfaces.web.org.dto.SlugAvailabilityResponseDto;
import com.lagab.eventz.app.util.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * OrganizationsController handles all organization-related operations including:
 * - Organization creation, reading, updating and deletion (CRUD)
 * - Organization membership verification
 * - Slug availability checking
 */
@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizations", description = "API for managing organizations including CRUD operations, membership verification, and slug availability checking")
@SecurityRequirement(name = "bearerAuth")
public class OrganizationController {

    public static final String MESSAGE = "message";
    private final OrganizationService organizationService;

    /**
     * Creates a new organization
     * POST /api/organizations
     *
     * @param request Organization creation data
     * @return JSON response with created organization or error
     */
    @Operation(
            summary = "Create a new organization",
            description = "Creates a new organization with the authenticated user as the owner. The user's email will be used as default if no email is provided in the request."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Organization successfully created",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OrganizationDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or validation errors",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\": \"Validation failed\", \"errors\": [\"Name is required\"]}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Organization slug already exists",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\": \"Slug already taken\"}")
                    )
            )
    })
    @PostMapping
    public ResponseEntity<OrganizationDto> createOrganization(
            @Parameter(description = "Organization creation data", required = true)
            @Valid @RequestBody CreateOrganizationRequestDto request) {

        User user = SecurityUtils.getCurrentUser();

        OrganizationDto org = new OrganizationDto(
                null,
                request.name(),
                request.slug(),
                !StringUtils.isEmpty(request.email()) ? request.email() : user.getEmail(),
                request.logo(),
                request.metadata());

        OrganizationDto organization = organizationService.createOrganization(org, user);

        return ResponseEntity.status(HttpStatus.CREATED).body(organization);

    }

    /**
     * Lists all organizations for the authenticated user
     * GET /api/organizations
     *
     * @return JSON response with organizations list or error
     */
    @Operation(
            summary = "List user organizations",
            description = "Retrieves all organizations where the authenticated user is a member (any role: owner, admin, or member)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved organizations list",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OrganizationDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content
            )
    })
    @GetMapping
    public ResponseEntity<List<OrganizationDto>> listOrganizations() {

        Long userId = SecurityUtils.getCurrentUserId();

        List<OrganizationDto> organizations = organizationService.getUserOrganizations(userId);

        return ResponseEntity.ok(organizations);

    }

    /**
     * Shows details of a specific organization
     * GET /api/organizations/{id}
     *
     * @param id Organization ID
     * @return JSON response with organization details or error
     */
    @Operation(
            summary = "Get organization details",
            description = "Retrieves detailed information about a specific organization. User must be a member of the organization to access this endpoint."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved organization details",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OrganizationDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - user is not a member of the organization",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\": \"User not allowed to access this organization\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Organization not found",
                    content = @Content
            )
    })
    @GetMapping("/{id}")
    @PreAuthorize("@organizationSecurityService.isMember(authentication.principal.id, #id)")
    public ResponseEntity<OrganizationDto> show(
            @Parameter(description = "Organization identifier", required = true, example = "org-123")
            @PathVariable String id) {

        Long userId = SecurityUtils.getCurrentUserId();

        // Get organization details
        OrganizationDto organization = organizationService.getOrganization(id);

        // Verify user has access to this organization
        if (organizationService.isUserMember(userId, id)) {
            return ResponseEntity.ok(organization);
        } else {
            throw new UnauthorizedException("User not allowed to access this organization");
        }

    }

    /**
     * Updates an existing organization
     * PUT /api/organizations/{id}
     *
     * @param id      Organization ID to update
     * @param request Update data
     * @return JSON response with updated organization or error
     */
    @Operation(
            summary = "Update organization",
            description = "Updates an existing organization's information. User must have admin role or higher in the organization to perform this action."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Organization successfully updated",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OrganizationDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or validation errors",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\": \"Validation failed\", \"errors\": [\"Name is required\"]}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - user does not have admin privileges in the organization",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Organization not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Slug conflict - the new slug is already taken by another organization",
                    content = @Content
            )
    })
    @PutMapping("/{id}")
    @PreAuthorize("@organizationSecurityService.isAdmin(authentication.principal.id, #id)")
    public ResponseEntity<OrganizationDto> update(
            @Parameter(description = "Organization identifier", required = true, example = "org-123")
            @PathVariable String id,
            @Parameter(description = "Updated organization data", required = true)
            @Valid @RequestBody OrganizationDto request) {

        Long userId = SecurityUtils.getCurrentUserId();

        OrganizationDto organization = organizationService.updateOrganization(
                id,
                request,
                userId
        );

        return ResponseEntity.ok(organization);

    }

    /**
     * Deletes an organization
     * DELETE /api/organizations/{id}
     *
     * @param id Organization ID to delete
     * @return JSON response with success message or error
     */
    @Operation(
            summary = "Delete organization",
            description = "Permanently deletes an organization and all its associated data. Only the organization owner can perform this action. This operation cannot be undone."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Organization successfully deleted",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\": \"Organization deleted successfully\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - only organization owners can delete organizations",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Organization not found",
                    content = @Content
            )
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("@organizationSecurityService.isOwner(authentication.principal.id, #id)")
    public ResponseEntity<Map<String, String>> remove(
            @Parameter(description = "Organization identifier", required = true, example = "org-123")
            @PathVariable String id) {

        Long userId = SecurityUtils.getCurrentUserId();

        organizationService.deleteOrganization(id, userId);

        return ResponseEntity.ok(Map.of(MESSAGE, "Organization deleted successfully"));

    }

    /**
     * Checks if a slug is available for use
     * GET /api/organizations/check-slug/{slug}
     *
     * @param slug Slug to check
     * @return JSON response indicating slug availability
     */
    @Operation(
            summary = "Check slug availability (path parameter)",
            description = "Checks if a given slug is available for use when creating a new organization. Returns conflict status if slug is already taken."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Slug is available",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\": \"Slug available\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Slug is already taken",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\": \"Slug already taken\"}")
                    )
            )
    })
    @GetMapping("/check-slug/{slug}")
    public ResponseEntity<Map<String, String>> checkSlug(
            @Parameter(description = "Slug to check for availability", required = true, example = "my-organization")
            @PathVariable String slug) {

        boolean slugExists = organizationService.isSlugTaken(slug);

        if (slugExists) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                                 .body(Map.of(MESSAGE, "Slug already taken"));
        }

        return ResponseEntity.ok(Map.of(MESSAGE, "Slug available"));

    }

    /**
     * Alternative endpoint for checking slug via query parameter
     * GET /api/organizations/check-slug?slug=example
     *
     * @param slug Slug to check (query parameter)
     * @return JSON response indicating slug availability
     */
    @Operation(
            summary = "Check slug availability (query parameter)",
            description = "Alternative endpoint to check if a given slug is available for use when creating a new organization. Returns detailed availability information."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Slug availability check completed",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SlugAvailabilityResponseDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Available slug",
                                            value = "{\"slug\": \"my-org\", \"available\": true, \"message\": \"Slug is available\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Taken slug",
                                            value = "{\"slug\": \"existing-org\", \"available\": false, \"message\": \"Slug is already taken\"}"
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Missing or invalid slug parameter",
                    content = @Content
            )
    })
    @GetMapping("/check-slug")
    public ResponseEntity<SlugAvailabilityResponseDto> checkSlugQuery(
            @Parameter(description = "Slug to check for availability", required = true, example = "my-organization")
            @RequestParam String slug) {

        boolean available = !organizationService.isSlugTaken(slug);

        SlugAvailabilityResponseDto response = new SlugAvailabilityResponseDto(
                slug,
                available,
                available ? "Slug is available" : "Slug is already taken"
        );

        return ResponseEntity.ok(response);

    }

}
