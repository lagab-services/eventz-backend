package com.lagab.eventz.app.org.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lagab.eventz.app.domain.org.model.OrganizationMembership;
import com.lagab.eventz.app.domain.org.model.OrganizationPermission;
import com.lagab.eventz.app.domain.org.model.OrganizationRole;
import com.lagab.eventz.app.domain.org.model.OrganizationRolePermission;
import com.lagab.eventz.app.domain.org.repository.OrganizationMembershipRepository;
import com.lagab.eventz.app.domain.org.repository.OrganizationRolePermissionRepository;
import com.lagab.eventz.app.domain.org.service.OrganizationPermissionService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationPermissionService Tests")
class OrganizationPermissionServiceTest {

    @Mock
    private OrganizationRolePermissionRepository rolePermissionRepository;

    @Mock
    private OrganizationMembershipRepository membershipRepository;

    @InjectMocks
    private OrganizationPermissionService organizationPermissionService;

    private static final String ORGANIZATION_ID = "org-123";
    private static final Long USER_ID = 1L;

    @Nested
    @DisplayName("Initialize Default Permissions Tests")
    class InitializeDefaultPermissionsTests {

        @Test
        @DisplayName("Should initialize all default permissions for all roles")
        void initializeDefaultPermissions_ShouldCreatePermissionsForAllRoles() {
            // When
            organizationPermissionService.initializeDefaultPermissions(ORGANIZATION_ID);

            // Then
            ArgumentCaptor<List<OrganizationRolePermission>> captor = ArgumentCaptor.forClass(List.class);
            verify(rolePermissionRepository, times(4)).saveAll(captor.capture());

            List<List<OrganizationRolePermission>> allSavedPermissions = captor.getAllValues();

            // Verify OWNER permissions (should be the most comprehensive)
            List<OrganizationRolePermission> ownerPermissions = allSavedPermissions.getFirst();
            assertTrue(ownerPermissions.size() >= 15); // OWNER should have most permissions
            assertTrue(ownerPermissions.stream().anyMatch(p -> p.getPermission() == OrganizationPermission.ORGANIZATION_DELETE));
            assertTrue(ownerPermissions.stream().anyMatch(p -> p.getPermission() == OrganizationPermission.FINANCE_MANAGE));

            // Verify ADMIN permissions
            List<OrganizationRolePermission> adminPermissions = allSavedPermissions.get(1);
            assertTrue(adminPermissions.size() > 5);
            assertTrue(adminPermissions.stream().anyMatch(p -> p.getPermission() == OrganizationPermission.MEMBER_INVITE));
            assertFalse(adminPermissions.stream().anyMatch(p -> p.getPermission() == OrganizationPermission.ORGANIZATION_DELETE));

            // Verify MEMBER permissions
            List<OrganizationRolePermission> memberPermissions = allSavedPermissions.get(2);
            assertTrue(memberPermissions.size() > 2);
            assertTrue(memberPermissions.stream().anyMatch(p -> p.getPermission() == OrganizationPermission.ORGANIZATION_VIEW));
            assertFalse(memberPermissions.stream().anyMatch(p -> p.getPermission() == OrganizationPermission.MEMBER_REMOVE));

            // Verify VIEWER permissions (should be minimal)
            List<OrganizationRolePermission> viewerPermissions = allSavedPermissions.get(3);
            assertEquals(2, viewerPermissions.size());
            assertTrue(viewerPermissions.stream().anyMatch(p -> p.getPermission() == OrganizationPermission.ORGANIZATION_VIEW));
            assertTrue(viewerPermissions.stream().anyMatch(p -> p.getPermission() == OrganizationPermission.MEMBER_VIEW));
        }

        @Test
        @DisplayName("Should set all permissions as granted")
        void initializeDefaultPermissions_ShouldSetAllPermissionsAsGranted() {
            // When
            organizationPermissionService.initializeDefaultPermissions(ORGANIZATION_ID);

            // Then
            ArgumentCaptor<List<OrganizationRolePermission>> captor = ArgumentCaptor.forClass(List.class);
            verify(rolePermissionRepository, times(4)).saveAll(captor.capture());

            captor.getAllValues().forEach(permissions ->
                    permissions.forEach(permission ->
                            assertTrue(permission.getGranted(), "All permissions should be granted by default")
                    )
            );
        }
    }

    @Nested
    @DisplayName("Permission Checking Tests")
    class PermissionCheckingTests {

        @Mock
        private OrganizationMembership membership;

        @Mock
        private OrganizationRolePermission rolePermission;

        @BeforeEach
        void setUp() {
            when(membershipRepository.findByUserIdAndOrganizationId(USER_ID, ORGANIZATION_ID))
                    .thenReturn(Optional.of(membership));
        }

        @Test
        @DisplayName("Should return true when user has the permission")
        void hasPermission_WithValidPermission_ShouldReturnTrue() {
            // Given
            when(membership.getRole()).thenReturn(OrganizationRole.ADMIN);
            when(rolePermissionRepository.findGrantedPermission(ORGANIZATION_ID, OrganizationRole.ADMIN, OrganizationPermission.MEMBER_VIEW))
                    .thenReturn(Optional.of(rolePermission));

            // When
            boolean result = organizationPermissionService.hasPermission(USER_ID, ORGANIZATION_ID, OrganizationPermission.MEMBER_VIEW);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when user doesn't have the permission")
        void hasPermission_WithoutPermission_ShouldReturnFalse() {
            // Given
            when(membership.getRole()).thenReturn(OrganizationRole.ADMIN);
            when(rolePermissionRepository.findGrantedPermission(ORGANIZATION_ID, OrganizationRole.ADMIN, OrganizationPermission.ORGANIZATION_DELETE))
                    .thenReturn(Optional.empty());

            // When
            boolean result = organizationPermissionService.hasPermission(USER_ID, ORGANIZATION_ID, OrganizationPermission.ORGANIZATION_DELETE);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false when user is not a member")
        void hasPermission_WithNonMember_ShouldReturnFalse() {
            // Given
            when(membershipRepository.findByUserIdAndOrganizationId(USER_ID, ORGANIZATION_ID))
                    .thenReturn(Optional.empty());

            // When
            boolean result = organizationPermissionService.hasPermission(USER_ID, ORGANIZATION_ID, OrganizationPermission.MEMBER_VIEW);

            // Then
            assertFalse(result);
            verify(rolePermissionRepository, never()).findGrantedPermission(any(), any(), any());
        }

        @Test
        @DisplayName("Should return false when exception occurs")
        void hasPermission_WithException_ShouldReturnFalse() {
            // Given
            when(membership.getRole()).thenReturn(OrganizationRole.ADMIN);
            when(rolePermissionRepository.findGrantedPermission(any(), any(), any()))
                    .thenThrow(new RuntimeException("Database error"));

            // When
            boolean result = organizationPermissionService.hasPermission(USER_ID, ORGANIZATION_ID, OrganizationPermission.MEMBER_VIEW);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should return true when user has all specified permissions")
        void hasAllPermissions_WithAllPermissions_ShouldReturnTrue() {
            // Given
            when(membership.getRole()).thenReturn(OrganizationRole.ADMIN);
            when(rolePermissionRepository.findGrantedPermission(eq(ORGANIZATION_ID), eq(OrganizationRole.ADMIN), any()))
                    .thenReturn(Optional.of(rolePermission));

            // When
            boolean result = organizationPermissionService.hasAllPermissions(
                    USER_ID, ORGANIZATION_ID,
                    OrganizationPermission.MEMBER_VIEW,
                    OrganizationPermission.EVENT_CREATE
            );

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when user is missing one permission")
        void hasAllPermissions_WithMissingPermission_ShouldReturnFalse() {
            // Given
            when(membership.getRole()).thenReturn(OrganizationRole.ADMIN);
            when(rolePermissionRepository.findGrantedPermission(ORGANIZATION_ID, OrganizationRole.ADMIN, OrganizationPermission.MEMBER_VIEW))
                    .thenReturn(Optional.of(rolePermission));
            when(rolePermissionRepository.findGrantedPermission(ORGANIZATION_ID, OrganizationRole.ADMIN, OrganizationPermission.ORGANIZATION_DELETE))
                    .thenReturn(Optional.empty());

            // When
            boolean result = organizationPermissionService.hasAllPermissions(
                    USER_ID, ORGANIZATION_ID,
                    OrganizationPermission.MEMBER_VIEW,
                    OrganizationPermission.ORGANIZATION_DELETE
            );

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false when user has none of the permissions")
        void hasAnyPermission_WithNoPermissions_ShouldReturnFalse() {
            // Given
            when(membership.getRole()).thenReturn(OrganizationRole.ADMIN);
            when(rolePermissionRepository.findGrantedPermission(eq(ORGANIZATION_ID), eq(OrganizationRole.ADMIN), any()))
                    .thenReturn(Optional.empty());

            // When
            boolean result = organizationPermissionService.hasAnyPermission(
                    USER_ID, ORGANIZATION_ID,
                    OrganizationPermission.ORGANIZATION_DELETE,
                    OrganizationPermission.FINANCE_MANAGE
            );

            // Then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Role and User Permissions Tests")
    class RoleAndUserPermissionsTests {

        @Test
        @DisplayName("Should return permissions for a role")
        void getRolePermissions_ShouldReturnGrantedPermissions() {
            // Given
            OrganizationRolePermission perm1 = mock(OrganizationRolePermission.class);
            OrganizationRolePermission perm2 = mock(OrganizationRolePermission.class);
            OrganizationRolePermission perm3 = mock(OrganizationRolePermission.class);

            when(perm1.getGranted()).thenReturn(true);
            when(perm1.getPermission()).thenReturn(OrganizationPermission.MEMBER_VIEW);
            when(perm3.getGranted()).thenReturn(true);
            when(perm3.getPermission()).thenReturn(OrganizationPermission.EVENT_CREATE);

            when(rolePermissionRepository.findByOrganizationIdAndRole(ORGANIZATION_ID, OrganizationRole.MEMBER))
                    .thenReturn(List.of(perm1, perm2, perm3));

            // When
            Set<OrganizationPermission> result = organizationPermissionService.getRolePermissions(ORGANIZATION_ID, OrganizationRole.MEMBER);

            // Then
            assertEquals(2, result.size());
            assertTrue(result.contains(OrganizationPermission.MEMBER_VIEW));
            assertTrue(result.contains(OrganizationPermission.EVENT_CREATE));
            assertFalse(result.contains(OrganizationPermission.MEMBER_REMOVE));
        }

        @Test
        @DisplayName("Should return user permissions based on their role")
        void getUserPermissions_WithValidUser_ShouldReturnRolePermissions() {
            // Given
            OrganizationMembership membership = mock(OrganizationMembership.class);
            when(membership.getRole()).thenReturn(OrganizationRole.ADMIN);
            when(membershipRepository.findByUserIdAndOrganizationId(USER_ID, ORGANIZATION_ID))
                    .thenReturn(Optional.of(membership));

            OrganizationRolePermission perm1 = mock(OrganizationRolePermission.class);
            when(perm1.getGranted()).thenReturn(true);
            when(perm1.getPermission()).thenReturn(OrganizationPermission.MEMBER_INVITE);

            when(rolePermissionRepository.findByOrganizationIdAndRole(ORGANIZATION_ID, OrganizationRole.ADMIN))
                    .thenReturn(List.of(perm1));

            // When
            Set<OrganizationPermission> result = organizationPermissionService.getUserPermissions(USER_ID, ORGANIZATION_ID);

            // Then
            assertEquals(1, result.size());
            assertTrue(result.contains(OrganizationPermission.MEMBER_INVITE));
        }

        @Test
        @DisplayName("Should return empty set for non-member")
        void getUserPermissions_WithNonMember_ShouldReturnEmptySet() {
            // Given
            when(membershipRepository.findByUserIdAndOrganizationId(USER_ID, ORGANIZATION_ID))
                    .thenReturn(Optional.empty());

            // When
            Set<OrganizationPermission> result = organizationPermissionService.getUserPermissions(USER_ID, ORGANIZATION_ID);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return user role")
        void getUserRole_WithValidMembership_ShouldReturnRole() {
            // Given
            OrganizationMembership membership = mock(OrganizationMembership.class);
            when(membership.getRole()).thenReturn(OrganizationRole.OWNER);
            when(membershipRepository.findByUserIdAndOrganizationId(USER_ID, ORGANIZATION_ID))
                    .thenReturn(Optional.of(membership));

            // When
            OrganizationRole result = organizationPermissionService.getUserRole(USER_ID, ORGANIZATION_ID);

            // Then
            assertEquals(OrganizationRole.OWNER, result);
        }

        @Test
        @DisplayName("Should return null for non-member")
        void getUserRole_WithNonMember_ShouldReturnNull() {
            // Given
            when(membershipRepository.findByUserIdAndOrganizationId(USER_ID, ORGANIZATION_ID))
                    .thenReturn(Optional.empty());

            // When
            OrganizationRole result = organizationPermissionService.getUserRole(USER_ID, ORGANIZATION_ID);

            // Then
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("Permission Management Tests")
    class PermissionManagementTests {

        @Test
        @DisplayName("Should update role permissions by deleting old and creating new")
        void updateRolePermissions_ShouldReplaceExistingPermissions() {
            // Given
            Set<OrganizationPermission> newPermissions = Set.of(
                    OrganizationPermission.MEMBER_VIEW,
                    OrganizationPermission.EVENT_CREATE,
                    OrganizationPermission.STATS_VIEW
            );

            // When
            organizationPermissionService.updateRolePermissions(ORGANIZATION_ID, OrganizationRole.MEMBER, newPermissions);

            // Then
            verify(rolePermissionRepository).deleteByOrganizationIdAndRole(ORGANIZATION_ID, OrganizationRole.MEMBER);

            ArgumentCaptor<List<OrganizationRolePermission>> captor = ArgumentCaptor.forClass(List.class);
            verify(rolePermissionRepository).saveAll(captor.capture());

            List<OrganizationRolePermission> savedPermissions = captor.getValue();
            assertEquals(3, savedPermissions.size());

            Set<OrganizationPermission> savedPermissionTypes = savedPermissions.stream()
                                                                               .map(OrganizationRolePermission::getPermission)
                                                                               .collect(java.util.stream.Collectors.toSet());

            assertTrue(savedPermissionTypes.contains(OrganizationPermission.MEMBER_VIEW));
            assertTrue(savedPermissionTypes.contains(OrganizationPermission.EVENT_CREATE));
            assertTrue(savedPermissionTypes.contains(OrganizationPermission.STATS_VIEW));

            // Verify all permissions are granted
            savedPermissions.forEach(permission ->
                    assertTrue(permission.getGranted(), "All new permissions should be granted")
            );
        }

        @Test
        @DisplayName("Should grant permission when it doesn't exist")
        void setPermission_WithNewPermission_ShouldCreateGrantedPermission() {
            // Given
            when(rolePermissionRepository.findByOrganizationIdAndRoleAndPermission(
                    ORGANIZATION_ID, OrganizationRole.MEMBER, OrganizationPermission.EVENT_DELETE))
                    .thenReturn(Optional.empty());

            // When
            organizationPermissionService.setPermission(
                    ORGANIZATION_ID, OrganizationRole.MEMBER, OrganizationPermission.EVENT_DELETE, true);

            // Then
            ArgumentCaptor<OrganizationRolePermission> captor = ArgumentCaptor.forClass(OrganizationRolePermission.class);
            verify(rolePermissionRepository).save(captor.capture());

            OrganizationRolePermission savedPermission = captor.getValue();
            assertEquals(ORGANIZATION_ID, savedPermission.getOrganizationId());
            assertEquals(OrganizationRole.MEMBER, savedPermission.getRole());
            assertEquals(OrganizationPermission.EVENT_DELETE, savedPermission.getPermission());
            assertTrue(savedPermission.getGranted());
        }

        @Test
        @DisplayName("Should update existing permission when granting")
        void setPermission_WithExistingPermission_ShouldUpdateGrantedStatus() {
            // Given
            OrganizationRolePermission existingPermission = mock(OrganizationRolePermission.class);
            when(rolePermissionRepository.findByOrganizationIdAndRoleAndPermission(
                    ORGANIZATION_ID, OrganizationRole.ADMIN, OrganizationPermission.MEMBER_REMOVE))
                    .thenReturn(Optional.of(existingPermission));

            // When
            organizationPermissionService.setPermission(
                    ORGANIZATION_ID, OrganizationRole.ADMIN, OrganizationPermission.MEMBER_REMOVE, true);

            // Then
            verify(existingPermission).setGranted(true);
            verify(rolePermissionRepository).save(existingPermission);
        }

        @Test
        @DisplayName("Should revoke existing permission")
        void setPermission_WithRevokeExistingPermission_ShouldUpdateToFalse() {
            // Given
            OrganizationRolePermission existingPermission = mock(OrganizationRolePermission.class);
            when(rolePermissionRepository.findByOrganizationIdAndRoleAndPermission(
                    ORGANIZATION_ID, OrganizationRole.ADMIN, OrganizationPermission.MEMBER_REMOVE))
                    .thenReturn(Optional.of(existingPermission));

            // When
            organizationPermissionService.setPermission(
                    ORGANIZATION_ID, OrganizationRole.ADMIN, OrganizationPermission.MEMBER_REMOVE, false);

            // Then
            verify(existingPermission).setGranted(false);
            verify(rolePermissionRepository).save(existingPermission);
        }

        @Test
        @DisplayName("Should not create permission when revoking non-existing permission")
        void setPermission_WithRevokeNonExistingPermission_ShouldNotCreatePermission() {
            // Given
            when(rolePermissionRepository.findByOrganizationIdAndRoleAndPermission(
                    ORGANIZATION_ID, OrganizationRole.VIEWER, OrganizationPermission.ORGANIZATION_DELETE))
                    .thenReturn(Optional.empty());

            // When
            organizationPermissionService.setPermission(
                    ORGANIZATION_ID, OrganizationRole.VIEWER, OrganizationPermission.ORGANIZATION_DELETE, false);

            // Then
            verify(rolePermissionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should delete all organization permissions")
        void deleteOrganizationPermissions_ShouldDeleteAllPermissions() {
            // When
            organizationPermissionService.deleteOrganizationPermissions(ORGANIZATION_ID);

            // Then
            verify(rolePermissionRepository).deleteByOrganizationId(ORGANIZATION_ID);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle empty permissions set in updateRolePermissions")
        void updateRolePermissions_WithEmptySet_ShouldDeleteAllAndSaveEmpty() {
            // Given
            Set<OrganizationPermission> emptyPermissions = Collections.emptySet();

            // When
            organizationPermissionService.updateRolePermissions(ORGANIZATION_ID, OrganizationRole.VIEWER, emptyPermissions);

            // Then
            verify(rolePermissionRepository).deleteByOrganizationIdAndRole(ORGANIZATION_ID, OrganizationRole.VIEWER);

            ArgumentCaptor<List<OrganizationRolePermission>> captor = ArgumentCaptor.forClass(List.class);
            verify(rolePermissionRepository).saveAll(captor.capture());

            assertTrue(captor.getValue().isEmpty());
        }

        @Test
        @DisplayName("Should handle null organization ID gracefully")
        void hasPermission_WithNullOrganizationId_ShouldReturnFalse() {
            // When
            boolean result = organizationPermissionService.hasPermission(USER_ID, null, OrganizationPermission.MEMBER_VIEW);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should handle null user ID gracefully")
        void hasPermission_WithNullUserId_ShouldReturnFalse() {
            // When
            boolean result = organizationPermissionService.hasPermission(null, ORGANIZATION_ID, OrganizationPermission.MEMBER_VIEW);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should handle varargs with no permissions in hasAllPermissions")
        void hasAllPermissions_WithNoPermissions_ShouldReturnTrue() {
            // When
            boolean result = organizationPermissionService.hasAllPermissions(USER_ID, ORGANIZATION_ID);

            // Then
            assertTrue(result); // Empty array should return true (vacuous truth)
        }

        @Test
        @DisplayName("Should handle varargs with no permissions in hasAnyPermission")
        void hasAnyPermission_WithNoPermissions_ShouldReturnFalse() {
            // When
            boolean result = organizationPermissionService.hasAnyPermission(USER_ID, ORGANIZATION_ID);

            // Then
            assertFalse(result); // Empty array should return false
        }
    }

    @Nested
    @DisplayName("Integration-like Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should work correctly for a complete user workflow")
        void completeUserWorkflow_ShouldWorkCorrectly() {
            // Given - User is an ADMIN
            OrganizationMembership membership = mock(OrganizationMembership.class);
            when(membership.getRole()).thenReturn(OrganizationRole.ADMIN);
            when(membershipRepository.findByUserIdAndOrganizationId(USER_ID, ORGANIZATION_ID))
                    .thenReturn(Optional.of(membership));

            // Admin has MEMBER_VIEW but not ORGANIZATION_DELETE
            when(rolePermissionRepository.findGrantedPermission(ORGANIZATION_ID, OrganizationRole.ADMIN, OrganizationPermission.MEMBER_VIEW))
                    .thenReturn(Optional.of(mock(OrganizationRolePermission.class)));
            when(rolePermissionRepository.findGrantedPermission(ORGANIZATION_ID, OrganizationRole.ADMIN, OrganizationPermission.ORGANIZATION_DELETE))
                    .thenReturn(Optional.empty());

            // When & Then
            assertTrue(organizationPermissionService.hasPermission(USER_ID, ORGANIZATION_ID, OrganizationPermission.MEMBER_VIEW));
            assertFalse(organizationPermissionService.hasPermission(USER_ID, ORGANIZATION_ID, OrganizationPermission.ORGANIZATION_DELETE));

            assertTrue(organizationPermissionService.hasAnyPermission(USER_ID, ORGANIZATION_ID,
                    OrganizationPermission.MEMBER_VIEW, OrganizationPermission.ORGANIZATION_DELETE));

            assertFalse(organizationPermissionService.hasAllPermissions(USER_ID, ORGANIZATION_ID,
                    OrganizationPermission.MEMBER_VIEW, OrganizationPermission.ORGANIZATION_DELETE));

            assertEquals(OrganizationRole.ADMIN, organizationPermissionService.getUserRole(USER_ID, ORGANIZATION_ID));
        }
    }
}
