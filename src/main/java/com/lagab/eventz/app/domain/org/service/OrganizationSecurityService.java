package com.lagab.eventz.app.domain.org.service;

import org.springframework.stereotype.Service;

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
     * Checks if the user can invite members to the organization
     *
     * @param userId         ID of the user
     * @param organizationId ID of the organization
     * @return true if the user can invite members
     */
    public boolean canInviteMembers(Long userId, String organizationId) {
        return isAdmin(userId, organizationId);
    }

    /**
     * Checks if the user can manage member roles
     *
     * @param userId         ID of the user
     * @param organizationId ID of the organization
     * @return true if the user can manage roles
     */
    public boolean canManageMembers(Long userId, String organizationId) {
        if (isAdmin(userId, organizationId) || isOwner(userId, organizationId)) {
            return true;
        }

        return false;
    }

    /**
     * Checks if the user can remove a specific member
     *
     * @param userId         ID of the user performing the action
     * @param organizationId ID of the organization
     * @param targetUserId   ID of the user to be removed
     * @return true if the user can remove the member
     */
    public boolean canRemoveMember(Long userId, String organizationId, Long targetUserId) {
        // A user can always remove themselves
        if (userId.equals(targetUserId)) {
            return isMember(userId, organizationId);
        }

        return canManageMembers(userId, organizationId);
    }

    /**
     * Checks if the user can view organization details
     *
     * @param userId         ID of the user
     * @param organizationId ID of the organization
     * @return true if the user can view details
     */
    public boolean canViewOrganization(Long userId, String organizationId) {
        return isMember(userId, organizationId);
    }

    /**
     * Checks if the user can view organization statistics
     *
     * @param userId         ID of the user
     * @param organizationId ID of the organization
     * @return true if the user can view statistics
     */
    public boolean canViewStats(Long userId, String organizationId) {
        return isAdmin(userId, organizationId);
    }

    /**
     * Checks if the user can archive/restore the organization
     *
     * @param userId         ID of the user
     * @param organizationId ID of the organization
     * @return true if the user can archive/restore
     */
    public boolean canArchiveOrganization(Long userId, String organizationId) {
        return isOwner(userId, organizationId);
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
