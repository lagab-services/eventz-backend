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
public class OrganizationController {

    private final OrganizationService organizationService;

    /**
     * Creates a new organization
     * POST /api/organizations
     *
     * @param request Organization creation data
     * @return JSON response with created organization or error
     */
    @PostMapping
    public ResponseEntity<OrganizationDto> createOrganization(@Valid @RequestBody CreateOrganizationRequestDto request) {

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
    @GetMapping("/{id}")
    @PreAuthorize("@organizationSecurityService.isMember(authentication.principal.id, #id)")
    public ResponseEntity<OrganizationDto> show(@PathVariable String id) {

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
    @PutMapping("/{id}")
    @PreAuthorize("@organizationSecurityService.isAdmin(authentication.principal.id, #id)")
    public ResponseEntity<OrganizationDto> update(
            @PathVariable String id,
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
    @DeleteMapping("/{id}")
    @PreAuthorize("@organizationSecurityService.isOwner(authentication.principal.id, #id)")
    public ResponseEntity<Map<String, String>> remove(@PathVariable String id) {

        Long userId = SecurityUtils.getCurrentUserId();

        organizationService.deleteOrganization(id, userId);

        return ResponseEntity.ok(Map.of("message", "Organization deleted successfully"));

    }

    /**
     * Checks if a slug is available for use
     * GET /api/organizations/check-slug/{slug}
     *
     * @param slug Slug to check
     * @return JSON response indicating slug availability
     */
    @GetMapping("/check-slug/{slug}")
    public ResponseEntity<Map<String, String>> checkSlug(@PathVariable String slug) {

        boolean slugExists = organizationService.isSlugTaken(slug);

        if (slugExists) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                                 .body(Map.of("message", "Slug already taken"));
        }

        return ResponseEntity.ok(Map.of("message", "Slug available"));

    }

    /**
     * Alternative endpoint for checking slug via query parameter
     * GET /api/organizations/check-slug?slug=example
     *
     * @param slug Slug to check (query parameter)
     * @return JSON response indicating slug availability
     */
    @GetMapping("/check-slug")
    public ResponseEntity<SlugAvailabilityResponseDto> checkSlugQuery(
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
