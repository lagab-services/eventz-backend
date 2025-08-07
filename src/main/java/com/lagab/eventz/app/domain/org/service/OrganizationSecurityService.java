package com.lagab.eventz.app.domain.org.service;

import org.springframework.stereotype.Service;

import com.lagab.eventz.app.domain.org.model.OrganizationPermission;
import com.lagab.eventz.app.domain.org.model.OrganizationRole;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Security service for organization permissions
 * Used by @PreAuthorize to check permissions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationSecurityService {

    private final OrganizationService organizationService;
    private final OrganizationMembershipService organizationMembershipService;
    private final OrganizationPermissionService permissionService;

    /**
     * Checks if the user can access to the organization
     *
     * @param userId         ID of the user
     * @param organizationId ID of the organization
     * @return true if the user can access
     */
    public boolean canAccess(Long userId, String organizationId) {
        try {
            log.debug("Checking if user {} can access to the organization {}", userId, organizationId);
            return organizationService.isUserMember(userId, organizationId);
        } catch (Exception e) {
            log.warn("Error checking access for user {} in organization {}: {}",
                    userId, organizationId, e.getMessage());
            return false;
        }
    }

    /**
     * Checks if the user is a member of the organization
     *
     * @param userId         ID of the user
     * @param organizationId ID of the organization
     * @return true if the user is a member
     */
    public boolean isMember(Long userId, String organizationId) {
        try {
            log.debug("Checking if user {} is member of organization {}", userId, organizationId);
            return organizationService.isUserMember(userId, organizationId);
        } catch (Exception e) {
            log.warn("Error checking membership for user {} in organization {}: {}",
                    userId, organizationId, e.getMessage());
            return false;
        }
    }

    /**
     * Checks if the user has admin privileges in the organization
     *
     * @param userId         ID of the user
     * @param organizationId ID of the organization
     * @return true if the user is an owner or admin
     */
    public boolean isAdmin(Long userId, String organizationId) {
        try {
            log.debug("Checking if user {} is admin of organization {}", userId, organizationId);
            return organizationMembershipService.isUserAdmin(userId, organizationId);
        } catch (Exception e) {
            log.warn("Error checking admin privileges for user {} in organization {}: {}",
                    userId, organizationId, e.getMessage());
            return false;
        }
    }

    /**
     * Checks if the user is the owner of the organization
     *
     * @param userId         ID of the user
     * @param organizationId ID of the organization
     * @return true if the user is the owner
     */
    public boolean isOwner(Long userId, String organizationId) {
        try {
            log.debug("Checking if user {} is owner of organization {}", userId, organizationId);
            return organizationMembershipService.isUserOwner(userId, organizationId);
        } catch (Exception e) {
            log.warn("Error checking ownership for user {} in organization {}: {}",
                    userId, organizationId, e.getMessage());
            return false;
        }
    }

    /**
     * Checks if the user has a specific role in the organization
     *
     * @param userId         ID of the user
     * @param organizationId ID of the organization
     * @param requiredRole   Required role
     * @return true if the user has the required role or a higher role
     */
    public boolean hasRole(Long userId, String organizationId, OrganizationRole requiredRole) {
        try {
            log.debug("Checking if user {} has role {} in organization {}",
                    userId, requiredRole, organizationId);

            OrganizationRole userRole = organizationMembershipService.getUserRole(userId, organizationId);
            return hasRequiredPermissionLevel(userRole, requiredRole);
        } catch (Exception e) {
            log.warn("Error checking role for user {} in organization {}: {}",
                    userId, organizationId, e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a user has a specific permission in an organization
     *
     * @param userId         the ID of the user to check
     * @param organizationId the ID of the organization
     * @param permission     the permission to check
     * @return true if the user has the permission, false otherwise
     */
    public boolean hasPermission(Long userId, String organizationId, OrganizationPermission permission) {
        return permissionService.hasPermission(userId, organizationId, permission);
    }

    /**
     * Checks if a user has all specified permissions in an organization
     *
     * @param userId         the ID of the user to check
     * @param organizationId the ID of the organization
     * @param permissions    the permissions to check
     * @return true if the user has all permissions, false otherwise
     */
    public boolean hasAllPermissions(Long userId, String organizationId, OrganizationPermission... permissions) {
        return permissionService.hasAllPermissions(userId, organizationId, permissions);
    }

    /**
     * Checks if a user has any of the specified permissions in an organization
     *
     * @param userId         the ID of the user to check
     * @param organizationId the ID of the organization
     * @param permissions    the permissions to check
     * @return true if the user has at least one of the permissions, false otherwise
     */
    public boolean hasAnyPermission(Long userId, String organizationId, OrganizationPermission... permissions) {
        return permissionService.hasAnyPermission(userId, organizationId, permissions);
    }

    /**
     * Checks if a user can invite members to an organization
     *
     * @param userId         the ID of the user to check
     * @param organizationId the ID of the organization
     * @return true if the user has the MEMBER_INVITE permission, false otherwise
     */
    public boolean canInviteMembers(Long userId, String organizationId) {
        return hasPermission(userId, organizationId, OrganizationPermission.MEMBER_INVITE);
    }

    /**
     * Checks if a user can manage organization members (edit roles or remove members)
     *
     * @param userId         the ID of the user to check
     * @param organizationId the ID of the organization
     * @return true if the user has either MEMBER_EDIT_ROLE or MEMBER_REMOVE permission, false otherwise
     */
    public boolean canManageMembers(Long userId, String organizationId) {
        return hasAnyPermission(userId, organizationId,
                OrganizationPermission.MEMBER_EDIT_ROLE,
                OrganizationPermission.MEMBER_REMOVE);
    }

    /**
     * Checks if a user can remove another member from the organization
     *
     * @param userId         the ID of the user attempting the removal
     * @param organizationId the ID of the organization
     * @param targetUserId   the ID of the user to be removed
     * @return true if:
     * - The user is removing themselves and is a member, OR
     * - The user has the MEMBER_REMOVE permission
     */
    public boolean canRemoveMember(Long userId, String organizationId, Long targetUserId) {
        // A user can always remove themselves
        if (userId.equals(targetUserId)) {
            return isMember(userId, organizationId);
        }

        return hasPermission(userId, organizationId, OrganizationPermission.MEMBER_REMOVE);
    }

    /**
     * Checks if a user can view an organization's details
     *
     * @param userId         the ID of the user to check
     * @param organizationId the ID of the organization
     * @return true if the user has the ORGANIZATION_VIEW permission, false otherwise
     */
    public boolean canViewOrganization(Long userId, String organizationId) {
        return hasPermission(userId, organizationId, OrganizationPermission.ORGANIZATION_VIEW);
    }

    /**
     * Checks if a user can view organization statistics
     *
     * @param userId         the ID of the user to check
     * @param organizationId the ID of the organization
     * @return true if the user has the STATS_VIEW permission, false otherwise
     */
    public boolean canViewStats(Long userId, String organizationId) {
        return hasPermission(userId, organizationId, OrganizationPermission.STATS_VIEW);
    }

    /**
     * Checks if a user can archive an organization
     *
     * @param userId         the ID of the user to check
     * @param organizationId the ID of the organization
     * @return true if the user has the ORGANIZATION_ARCHIVE permission, false otherwise
     */
    public boolean canArchiveOrganization(Long userId, String organizationId) {
        return hasPermission(userId, organizationId, OrganizationPermission.ORGANIZATION_ARCHIVE);
    }

    /**
     * Checks if a user can edit an organization's details
     *
     * @param userId         the ID of the user to check
     * @param organizationId the ID of the organization
     * @return true if the user has the ORGANIZATION_EDIT permission, false otherwise
     */
    public boolean canEditOrganization(Long userId, String organizationId) {
        return hasPermission(userId, organizationId, OrganizationPermission.ORGANIZATION_EDIT);
    }

    /**
     * Checks if a user can delete an organization
     *
     * @param userId         the ID of the user to check
     * @param organizationId the ID of the organization
     * @return true if the user has the ORGANIZATION_DELETE permission, false otherwise
     */
    public boolean canDeleteOrganization(Long userId, String organizationId) {
        return hasPermission(userId, organizationId, OrganizationPermission.ORGANIZATION_DELETE);
    }

    /**
     * Checks if a user can manage organization settings
     *
     * @param userId         the ID of the user to check
     * @param organizationId the ID of the organization
     * @return true if the user has the SETTINGS_MANAGE permission, false otherwise
     */
    public boolean canManageSettings(Long userId, String organizationId) {
        return hasPermission(userId, organizationId, OrganizationPermission.SETTINGS_MANAGE);
    }

    /**
     * Determines if a user role meets the required permission level
     *
     * @param userRole     User's role
     * @param requiredRole Required role
     * @return true if the user role is sufficient
     */
    private boolean hasRequiredPermissionLevel(OrganizationRole userRole, OrganizationRole requiredRole) {
        if (userRole == null || requiredRole == null) {
            return false;
        }

        // Role hierarchy: OWNER > ADMIN > MEMBER > VIEWER
        int userLevel = getRoleLevel(userRole);
        int requiredLevel = getRoleLevel(requiredRole);

        return userLevel >= requiredLevel;
    }

    /**
     * Returns the numeric level of a role for comparison
     *
     * @param role Role to evaluate
     * @return Numeric level (higher = more permissions)
     */
    private int getRoleLevel(OrganizationRole role) {
        return switch (role) {
            case OWNER -> 4;
            case ADMIN -> 3;
            case MEMBER -> 2;
            case VIEWER -> 1;
        };
    }
}
