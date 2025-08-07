package com.lagab.eventz.app.domain.org.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lagab.eventz.app.domain.org.model.OrganizationMembership;
import com.lagab.eventz.app.domain.org.model.OrganizationPermission;
import com.lagab.eventz.app.domain.org.model.OrganizationRole;
import com.lagab.eventz.app.domain.org.model.OrganizationRolePermission;
import com.lagab.eventz.app.domain.org.repository.OrganizationMembershipRepository;
import com.lagab.eventz.app.domain.org.repository.OrganizationRolePermissionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrganizationPermissionService {

    private final OrganizationRolePermissionRepository rolePermissionRepository;
    private final OrganizationMembershipRepository membershipRepository;

    /**
     * Initialize default permissions for a new organization
     */
    public void initializeDefaultPermissions(String organizationId) {
        // Permissions for OWNER
        Set<OrganizationPermission> ownerPermissions = Set.of(
                OrganizationPermission.ORGANIZATION_VIEW,
                OrganizationPermission.ORGANIZATION_EDIT,
                OrganizationPermission.ORGANIZATION_DELETE,
                OrganizationPermission.ORGANIZATION_ARCHIVE,
                OrganizationPermission.MEMBER_VIEW,
                OrganizationPermission.MEMBER_INVITE,
                OrganizationPermission.MEMBER_EDIT_ROLE,
                OrganizationPermission.MEMBER_REMOVE,
                OrganizationPermission.STATS_VIEW,
                OrganizationPermission.STATS_EXPORT,
                OrganizationPermission.EVENT_CREATE,
                OrganizationPermission.EVENT_EDIT,
                OrganizationPermission.EVENT_DELETE,
                OrganizationPermission.EVENT_PUBLISH,
                OrganizationPermission.FINANCE_VIEW,
                OrganizationPermission.FINANCE_MANAGE,
                OrganizationPermission.SETTINGS_VIEW,
                OrganizationPermission.SETTINGS_MANAGE
        );

        // Permissions for ADMIN
        Set<OrganizationPermission> adminPermissions = Set.of(
                OrganizationPermission.ORGANIZATION_VIEW,
                OrganizationPermission.ORGANIZATION_EDIT,
                OrganizationPermission.MEMBER_VIEW,
                OrganizationPermission.MEMBER_INVITE,
                OrganizationPermission.MEMBER_EDIT_ROLE,
                OrganizationPermission.MEMBER_REMOVE,
                OrganizationPermission.STATS_VIEW,
                OrganizationPermission.EVENT_CREATE,
                OrganizationPermission.EVENT_EDIT,
                OrganizationPermission.EVENT_DELETE,
                OrganizationPermission.EVENT_PUBLISH,
                OrganizationPermission.SETTINGS_VIEW
        );

        // Permissions for MEMBER
        Set<OrganizationPermission> memberPermissions = Set.of(
                OrganizationPermission.ORGANIZATION_VIEW,
                OrganizationPermission.MEMBER_VIEW,
                OrganizationPermission.EVENT_CREATE,
                OrganizationPermission.EVENT_EDIT
        );

        // Permissions for VIEWER
        Set<OrganizationPermission> viewerPermissions = Set.of(
                OrganizationPermission.ORGANIZATION_VIEW,
                OrganizationPermission.MEMBER_VIEW
        );

        // Create permissions for each role
        createPermissionsForRole(organizationId, OrganizationRole.OWNER, ownerPermissions);
        createPermissionsForRole(organizationId, OrganizationRole.ADMIN, adminPermissions);
        createPermissionsForRole(organizationId, OrganizationRole.MEMBER, memberPermissions);
        createPermissionsForRole(organizationId, OrganizationRole.VIEWER, viewerPermissions);

        log.info("Default permissions initialized for organization: {}", organizationId);
    }

    /**
     * Create permissions for a specific role
     */
    private void createPermissionsForRole(String organizationId, OrganizationRole role, Set<OrganizationPermission> permissions) {
        List<OrganizationRolePermission> rolePermissions = permissions.stream()
                                                                      .map(permission -> new OrganizationRolePermission(organizationId, role,
                                                                              permission, true))
                                                                      .collect(Collectors.toList());

        rolePermissionRepository.saveAll(rolePermissions);
    }

    /**
     * Check if a user has a specific permission in an organization
     */
    @Transactional(readOnly = true)
    public boolean hasPermission(Long userId, String organizationId, OrganizationPermission permission) {
        try {
            // Get user's role
            OrganizationRole userRole = getUserRole(userId, organizationId);
            if (userRole == null) {
                return false;
            }

            // Check if permission is granted for this role
            Optional<OrganizationRolePermission> rolePermission = rolePermissionRepository
                    .findGrantedPermission(organizationId, userRole, permission);

            return rolePermission.isPresent();
        } catch (Exception e) {
            log.warn("Error checking permission {} for user {} in organization {}: {}",
                    permission, userId, organizationId, e.getMessage());
            return false;
        }
    }

    /**
     * Check if a user has all specified permissions
     */
    @Transactional(readOnly = true)
    public boolean hasAllPermissions(Long userId, String organizationId, OrganizationPermission... permissions) {
        return Arrays.stream(permissions)
                     .allMatch(permission -> hasPermission(userId, organizationId, permission));
    }

    /**
     * Check if a user has at least one of the specified permissions
     */
    @Transactional(readOnly = true)
    public boolean hasAnyPermission(Long userId, String organizationId, OrganizationPermission... permissions) {
        return Arrays.stream(permissions)
                     .anyMatch(permission -> hasPermission(userId, organizationId, permission));
    }

    /**
     * Get all permissions for a role in an organization
     */
    @Transactional(readOnly = true)
    public Set<OrganizationPermission> getRolePermissions(String organizationId, OrganizationRole role) {
        List<OrganizationRolePermission> rolePermissions = rolePermissionRepository
                .findByOrganizationIdAndRole(organizationId, role);

        return rolePermissions.stream()
                              .filter(OrganizationRolePermission::getGranted)
                              .map(OrganizationRolePermission::getPermission)
                              .collect(Collectors.toSet());
    }

    /**
     * Get all permissions for a user in an organization
     */
    @Transactional(readOnly = true)
    public Set<OrganizationPermission> getUserPermissions(Long userId, String organizationId) {
        OrganizationRole userRole = getUserRole(userId, organizationId);
        if (userRole == null) {
            return Collections.emptySet();
        }

        return getRolePermissions(organizationId, userRole);
    }

    /**
     * Update permissions for a role in an organization
     */
    public void updateRolePermissions(String organizationId, OrganizationRole role, Set<OrganizationPermission> permissions) {
        // Delete old permissions
        rolePermissionRepository.deleteByOrganizationIdAndRole(organizationId, role);

        // Create new permissions
        List<OrganizationRolePermission> rolePermissions = permissions.stream()
                                                                      .map(permission -> new OrganizationRolePermission(organizationId, role,
                                                                              permission, true))
                                                                      .collect(Collectors.toList());

        rolePermissionRepository.saveAll(rolePermissions);

        log.info("Updated permissions for role {} in organization {}", role, organizationId);
    }

    /**
     * Grant or revoke a specific permission for a role
     */
    public void setPermission(String organizationId, OrganizationRole role, OrganizationPermission permission, boolean granted) {
        Optional<OrganizationRolePermission> existingPermission = rolePermissionRepository
                .findByOrganizationIdAndRoleAndPermission(organizationId, role, permission);

        if (existingPermission.isPresent()) {
            // Update existing permission
            existingPermission.get().setGranted(granted);
            rolePermissionRepository.save(existingPermission.get());
        } else if (granted) {
            // Create new permission if it should be granted
            OrganizationRolePermission newPermission = new OrganizationRolePermission(
                    organizationId, role, permission, true);
            rolePermissionRepository.save(newPermission);
        }

        log.info("Permission {} {} for role {} in organization {}",
                permission, granted ? "granted" : "revoked", role, organizationId);
    }

    /**
     * Delete all permissions for an organization (when deleting)
     */
    public void deleteOrganizationPermissions(String organizationId) {
        rolePermissionRepository.deleteByOrganizationId(organizationId);
        log.info("Deleted all permissions for organization: {}", organizationId);
    }

    @Transactional(readOnly = true)
    public OrganizationRole getUserRole(Long userId, String organizationId) {
        Optional<OrganizationMembership> membership = membershipRepository
                .findByUserIdAndOrganizationId(userId, organizationId);

        return membership.map(OrganizationMembership::getRole).orElse(null);
    }
}
