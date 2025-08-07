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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/organizations/{organizationId}/permissions")
@RequiredArgsConstructor
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
    @GetMapping("/roles/{role}")
    public ResponseEntity<OrganizationRolePermissionsDto> getRolePermissions(
            @PathVariable String organizationId,
            @PathVariable OrganizationRole role) {

        Long userId = SecurityUtils.getCurrentUserId();

        // Check if user can view settings
        if (!securityService.hasPermission(userId, organizationId, OrganizationPermission.SETTINGS_VIEW)) {
            throw new AccessDeniedException("You do not have permission to see organization permissions");
        }

        Set<OrganizationPermission> permissions = permissionService.getRolePermissions(organizationId, role);

        return ResponseEntity.ok(new OrganizationRolePermissionsDto(role, permissions));
    }

    /**
     * Retrieves all available permissions in the system
     *
     * @param organizationId the ID of the organization
     * @return ResponseEntity containing the list of available permissions or 403 if unauthorized
     */
    @GetMapping("/available")
    public ResponseEntity<List<PermissionDto>> getAvailablePermissions(
            @PathVariable String organizationId) {

        Long userId = SecurityUtils.getCurrentUserId();

        // Check if user can view settings
        if (!securityService.hasPermission(userId, organizationId, OrganizationPermission.SETTINGS_VIEW)) {
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
     * @param organizationId the ID of the organization
     * @return ResponseEntity containing the user's permissions or 403 if unauthorized
     */
    @GetMapping("/my-permissions")
    public ResponseEntity<Set<OrganizationPermission>> getMyPermissions(
            @PathVariable String organizationId) {

        Long userId = SecurityUtils.getCurrentUserId();

        // Check if user is a member of the organization
        if (!securityService.isMember(userId, organizationId)) {
            throw new AccessDeniedException("You do not have permission to this organization");
        }

        Set<OrganizationPermission> permissions = permissionService.getUserPermissions(userId, organizationId);

        return ResponseEntity.ok(permissions);
    }

    /**
     * Grants or revokes a specific permission for a role
     *
     * @param organizationId the ID of the organization
     * @param role           the role to modify
     * @param permission     the permission to grant or revoke
     * @param granted        true to grant, false to revoke
     * @return ResponseEntity with 200 OK if successful or 403 if unauthorized
     */
    @PatchMapping("/roles/{role}/permissions/{permission}")
    public ResponseEntity<Void> setRolePermission(
            @PathVariable String organizationId,
            @PathVariable OrganizationRole role,
            @PathVariable OrganizationPermission permission,
            @RequestParam boolean granted) {

        Long userId = SecurityUtils.getCurrentUserId();

        // Check if user can manage settings
        if (!securityService.hasPermission(userId, organizationId, OrganizationPermission.SETTINGS_MANAGE)) {
            throw new AccessDeniedException("You do not have permission to set role permission");
        }

        // Prevent modification of OWNER permissions except by an OWNER
        if (role == OrganizationRole.OWNER && !securityService.isOwner(userId, organizationId)) {
            throw new AccessDeniedException("You do not have permission to set role permission");
        }

        permissionService.setPermission(organizationId, role, permission, granted);

        return ResponseEntity.ok().build();
    }

    /**
     * Updates the permissions for a specific role in the organization
     *
     * @param organizationId the ID of the organization
     * @param role           the role to update permissions for
     * @param updateDto      DTO containing the new permissions to set
     * @return ResponseEntity containing the updated role permissions or 403 if unauthorized
     * @throws org.springframework.web.bind.MethodArgumentNotValidException if validation fails
     * @throws org.springframework.security.access.AccessDeniedException    if user lacks permission
     */
    @PutMapping("/roles/{role}")
    public ResponseEntity<OrganizationRolePermissionsDto> updateRolePermissions(
            @PathVariable String organizationId,
            @PathVariable OrganizationRole role,
            @RequestBody @Valid UpdateRolePermissionsDto updateDto) {

        Long userId = SecurityUtils.getCurrentUserId();

        // Verify user can manage settings
        if (!securityService.hasPermission(userId, organizationId, OrganizationPermission.SETTINGS_MANAGE)) {
            throw new AccessDeniedException("You do not have permission to set role permission");
        }

        // Prevent modification of OWNER permissions except by an OWNER
        if (role == OrganizationRole.OWNER && !securityService.isOwner(userId, organizationId)) {
            throw new AccessDeniedException("You do not have permission to set role permission");
        }

        permissionService.updateRolePermissions(organizationId, role, updateDto.permissions());

        Set<OrganizationPermission> updatedPermissions = permissionService.getRolePermissions(organizationId, role);

        return ResponseEntity.ok(new OrganizationRolePermissionsDto(role, updatedPermissions));
    }

}
