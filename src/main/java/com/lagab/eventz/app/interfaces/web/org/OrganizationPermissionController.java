package com.lagab.eventz.app.interfaces.web.org;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lagab.eventz.app.domain.org.dto.permission.OrganizationRolePermissionsDto;
import com.lagab.eventz.app.domain.org.dto.permission.PermissionDto;
import com.lagab.eventz.app.domain.org.dto.permission.UpdateRolePermissionsDto;
import com.lagab.eventz.app.domain.org.model.OrganizationPermission;
import com.lagab.eventz.app.domain.org.model.OrganizationRole;
import com.lagab.eventz.app.domain.org.service.OrganizationPermissionService;
import com.lagab.eventz.app.domain.org.service.OrganizationSecurityService;
import com.lagab.eventz.app.util.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/organizations/{orgId}/permissions")
@RequiredArgsConstructor
@Tag(name = "Organization Permissions", description = "API for managing organization role permissions and access control")
public class OrganizationPermissionController {

    private final OrganizationPermissionService permissionService;
    private final OrganizationSecurityService securityService;

    /**
     * Retrieves all permissions for a specific role in the organization
     *
     * @param organizationId the ID of the organization
     * @param role           the role to get permissions for
     * @return ResponseEntity containing the role permissions or 403 if unauthorized
     */
    @Operation(
            summary = "Get role permissions",
            description = "Retrieves all permissions assigned to a specific role within an organization. Requires SETTINGS_VIEW permission."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved role permissions",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OrganizationRolePermissionsDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - insufficient permissions to view organization settings",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Organization or role not found",
                    content = @Content
            )
    })
    @GetMapping("/roles/{role}")
    public ResponseEntity<OrganizationRolePermissionsDto> getRolePermissions(
            @Parameter(description = "Organization identifier", required = true, example = "org-123")
            @PathVariable String orgId,
            @Parameter(description = "Organization role to get permissions for", required = true)
            @PathVariable OrganizationRole role) {

        Long userId = SecurityUtils.getCurrentUserId();

        // Check if user can view settings
        if (!securityService.hasPermission(userId, orgId, OrganizationPermission.SETTINGS_VIEW)) {
            throw new AccessDeniedException("You do not have permission to see organization permissions");
        }

        Set<OrganizationPermission> permissions = permissionService.getRolePermissions(orgId, role);

        return ResponseEntity.ok(new OrganizationRolePermissionsDto(role, permissions));
    }

    /**
     * Retrieves all available permissions in the system
     *
     * @param orgId the ID of the organization
     * @return ResponseEntity containing the list of available permissions or 403 if unauthorized
     */
    @Operation(
            summary = "Get available permissions",
            description = "Retrieves all permissions that can be assigned to roles within the organization. Requires SETTINGS_VIEW permission."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved available permissions",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PermissionDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - insufficient permissions to view organization settings",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Organization not found",
                    content = @Content
            )
    })
    @GetMapping("/available")
    public ResponseEntity<List<PermissionDto>> getAvailablePermissions(
            @Parameter(description = "Organization identifier", required = true, example = "org-123")
            @PathVariable String orgId) {

        Long userId = SecurityUtils.getCurrentUserId();

        // Check if user can view settings
        if (!securityService.hasPermission(userId, orgId, OrganizationPermission.SETTINGS_VIEW)) {
            throw new AccessDeniedException("You do not have permission to see organization permissions");
        }

        List<PermissionDto> permissions = Arrays.stream(OrganizationPermission.values())
                                                .map(permission -> new PermissionDto(permission, permission.getDescription(), false))
                                                .toList();

        return ResponseEntity.ok(permissions);
    }

    /**
     * Retrieves the permissions of the currently authenticated user
     *
     * @param orgId the ID of the organization
     * @return ResponseEntity containing the user's permissions or 403 if unauthorized
     */
    @Operation(
            summary = "Get current user permissions",
            description = "Retrieves all permissions that the currently authenticated user has within the specified organization."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved user permissions",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OrganizationPermission.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - user is not a member of the organization",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Organization not found",
                    content = @Content
            )
    })
    @GetMapping("/my-permissions")
    public ResponseEntity<Set<OrganizationPermission>> getMyPermissions(
            @Parameter(description = "Organization identifier", required = true, example = "org-123")
            @PathVariable String orgId) {

        Long userId = SecurityUtils.getCurrentUserId();

        // Check if user is a member of the organization
        if (!securityService.isMember(userId, orgId)) {
            throw new AccessDeniedException("You do not have permission to this organization");
        }

        Set<OrganizationPermission> permissions = permissionService.getUserPermissions(userId, orgId);

        return ResponseEntity.ok(permissions);
    }

    /**
     * Grants or revokes a specific permission for a role
     *
     * @param orgId      the ID of the organization
     * @param role       the role to modify
     * @param permission the permission to grant or revoke
     * @param granted    true to grant, false to revoke
     * @return ResponseEntity with 200 OK if successful or 403 if unauthorized
     */
    @Operation(
            summary = "Set role permission",
            description = "Grants or revokes a specific permission for a role within the organization. Requires SETTINGS_MANAGE permission. Only owners can modify owner role permissions."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Permission successfully updated"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - insufficient permissions to manage settings or modify owner role",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Organization, role, or permission not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters",
                    content = @Content
            )
    })
    @PatchMapping("/roles/{role}/permissions/{permission}")
    public ResponseEntity<Void> setRolePermission(
            @Parameter(description = "Organization identifier", required = true, example = "org-123")
            @PathVariable String orgId,
            @Parameter(description = "Organization role to modify", required = true)
            @PathVariable OrganizationRole role,
            @Parameter(description = "Permission to grant or revoke", required = true)
            @PathVariable OrganizationPermission permission,
            @Parameter(description = "Whether to grant (true) or revoke (false) the permission", required = true, example = "true")
            @RequestParam boolean granted) {

        Long userId = SecurityUtils.getCurrentUserId();

        // Check if user can manage settings
        if (!securityService.hasPermission(userId, orgId, OrganizationPermission.SETTINGS_MANAGE)) {
            throw new AccessDeniedException("You do not have permission to set role permission");
        }

        // Prevent modification of OWNER permissions except by an OWNER
        if (role == OrganizationRole.OWNER && !securityService.isOwner(userId, orgId)) {
            throw new AccessDeniedException("You do not have permission to set role permission");
        }

        permissionService.setPermission(orgId, role, permission, granted);

        return ResponseEntity.ok().build();
    }

    /**
     * Updates the permissions for a specific role in the organization
     *
     * @param orgId     the ID of the organization
     * @param role      the role to update permissions for
     * @param updateDto DTO containing the new permissions to set
     * @return ResponseEntity containing the updated role permissions or 403 if unauthorized
     * @throws org.springframework.web.bind.MethodArgumentNotValidException if validation fails
     * @throws org.springframework.security.access.AccessDeniedException    if user lacks permission
     */
    @Operation(
            summary = "Update role permissions",
            description = "Updates all permissions for a specific role within the organization. This replaces all existing permissions with the provided set. Requires SETTINGS_MANAGE permission. Only owners can modify owner role permissions."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Role permissions successfully updated",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OrganizationRolePermissionsDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request body or validation errors",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - insufficient permissions to manage settings or modify owner role",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Organization or role not found",
                    content = @Content
            )
    })
    @PutMapping("/roles/{role}")
    public ResponseEntity<OrganizationRolePermissionsDto> updateRolePermissions(
            @Parameter(description = "Organization identifier", required = true, example = "org-123")
            @PathVariable String orgId,
            @Parameter(description = "Organization role to update permissions for", required = true)
            @PathVariable OrganizationRole role,
            @Parameter(description = "DTO containing the new set of permissions to assign to the role", required = true)
            @RequestBody @Valid UpdateRolePermissionsDto updateDto) {

        Long userId = SecurityUtils.getCurrentUserId();

        // Verify user can manage settings
        if (!securityService.hasPermission(userId, orgId, OrganizationPermission.SETTINGS_MANAGE)) {
            throw new AccessDeniedException("You do not have permission to set role permission");
        }

        // Prevent modification of OWNER permissions except by an OWNER
        if (role == OrganizationRole.OWNER && !securityService.isOwner(userId, orgId)) {
            throw new AccessDeniedException("You do not have permission to set role permission");
        }

        permissionService.updateRolePermissions(orgId, role, updateDto.permissions());

        Set<OrganizationPermission> updatedPermissions = permissionService.getRolePermissions(orgId, role);

        return ResponseEntity.ok(new OrganizationRolePermissionsDto(role, updatedPermissions));
    }

}
