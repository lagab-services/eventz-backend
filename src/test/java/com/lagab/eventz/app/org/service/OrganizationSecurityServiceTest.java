package com.lagab.eventz.app.org.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lagab.eventz.app.domain.org.model.OrganizationRole;
import com.lagab.eventz.app.domain.org.service.OrganizationMembershipService;
import com.lagab.eventz.app.domain.org.service.OrganizationSecurityService;
import com.lagab.eventz.app.domain.org.service.OrganizationService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationSecurityService Tests")
class OrganizationSecurityServiceTest {

    @Mock
    private OrganizationService organizationService;

    @Mock
    private OrganizationMembershipService organizationMembershipService;

    @InjectMocks
    private OrganizationSecurityService organizationSecurityService;

    private static final Long USER_ID = 1L;
    private static final String ORG_ID = "org-123";
    private static final Long TARGET_USER_ID = 2L;

    @Nested
    @DisplayName("isMember() tests")
    class IsMemberTests {

        @Test
        @DisplayName("Should return true when user is a member")
        void shouldReturnTrueWhenUserIsMember() {
            // Given
            when(organizationService.isUserMember(USER_ID, ORG_ID)).thenReturn(true);

            // When
            boolean result = organizationSecurityService.isMember(USER_ID, ORG_ID);

            // Then
            assertTrue(result);
            verify(organizationService).isUserMember(USER_ID, ORG_ID);
        }

        @Test
        @DisplayName("Should return false when user is not a member")
        void shouldReturnFalseWhenUserIsNotMember() {
            // Given
            when(organizationService.isUserMember(USER_ID, ORG_ID)).thenReturn(false);

            // When
            boolean result = organizationSecurityService.isMember(USER_ID, ORG_ID);

            // Then
            assertFalse(result);
            verify(organizationService).isUserMember(USER_ID, ORG_ID);
        }

        @Test
        @DisplayName("Should return false when exception is thrown")
        void shouldReturnFalseWhenExceptionThrown() {
            // Given
            when(organizationService.isUserMember(USER_ID, ORG_ID))
                    .thenThrow(new RuntimeException("Database error"));

            // When
            boolean result = organizationSecurityService.isMember(USER_ID, ORG_ID);

            // Then
            assertFalse(result);
            verify(organizationService).isUserMember(USER_ID, ORG_ID);
        }
    }

    @Nested
    @DisplayName("isAdmin() tests")
    class IsAdminTests {

        @Test
        @DisplayName("Should return true when user is admin")
        void shouldReturnTrueWhenUserIsAdmin() {
            // Given
            when(organizationMembershipService.isUserAdmin(USER_ID, ORG_ID)).thenReturn(true);

            // When
            boolean result = organizationSecurityService.isAdmin(USER_ID, ORG_ID);

            // Then
            assertTrue(result);
            verify(organizationMembershipService).isUserAdmin(USER_ID, ORG_ID);
        }

        @Test
        @DisplayName("Should return false when user is not admin")
        void shouldReturnFalseWhenUserIsNotAdmin() {
            // Given
            when(organizationMembershipService.isUserAdmin(USER_ID, ORG_ID)).thenReturn(false);

            // When
            boolean result = organizationSecurityService.isAdmin(USER_ID, ORG_ID);

            // Then
            assertFalse(result);
            verify(organizationMembershipService).isUserAdmin(USER_ID, ORG_ID);
        }

        @Test
        @DisplayName("Should return false when exception is thrown")
        void shouldReturnFalseWhenExceptionThrown() {
            // Given
            when(organizationMembershipService.isUserAdmin(USER_ID, ORG_ID))
                    .thenThrow(new RuntimeException("Service error"));

            // When
            boolean result = organizationSecurityService.isAdmin(USER_ID, ORG_ID);

            // Then
            assertFalse(result);
            verify(organizationMembershipService).isUserAdmin(USER_ID, ORG_ID);
        }
    }

    @Nested
    @DisplayName("isOwner() tests")
    class IsOwnerTests {

        @Test
        @DisplayName("Should return true when user is owner")
        void shouldReturnTrueWhenUserIsOwner() {
            // Given
            when(organizationMembershipService.isUserOwner(USER_ID, ORG_ID)).thenReturn(true);

            // When
            boolean result = organizationSecurityService.isOwner(USER_ID, ORG_ID);

            // Then
            assertTrue(result);
            verify(organizationMembershipService).isUserOwner(USER_ID, ORG_ID);
        }

        @Test
        @DisplayName("Should return false when user is not owner")
        void shouldReturnFalseWhenUserIsNotOwner() {
            // Given
            when(organizationMembershipService.isUserOwner(USER_ID, ORG_ID)).thenReturn(false);

            // When
            boolean result = organizationSecurityService.isOwner(USER_ID, ORG_ID);

            // Then
            assertFalse(result);
            verify(organizationMembershipService).isUserOwner(USER_ID, ORG_ID);
        }

        @Test
        @DisplayName("Should return false when exception is thrown")
        void shouldReturnFalseWhenExceptionThrown() {
            // Given
            when(organizationMembershipService.isUserOwner(USER_ID, ORG_ID))
                    .thenThrow(new RuntimeException("Service error"));

            // When
            boolean result = organizationSecurityService.isOwner(USER_ID, ORG_ID);

            // Then
            assertFalse(result);
            verify(organizationMembershipService).isUserOwner(USER_ID, ORG_ID);
        }
    }

    @Nested
    @DisplayName("hasRole() tests")
    class HasRoleTests {

        @Test
        @DisplayName("Should return true when user has exact required role")
        void shouldReturnTrueWhenUserHasExactRole() {
            // Given
            when(organizationMembershipService.getUserRole(USER_ID, ORG_ID))
                    .thenReturn(OrganizationRole.ADMIN);

            // When
            boolean result = organizationSecurityService.hasRole(USER_ID, ORG_ID, OrganizationRole.ADMIN);

            // Then
            assertTrue(result);
            verify(organizationMembershipService).getUserRole(USER_ID, ORG_ID);
        }

        @Test
        @DisplayName("Should return true when user has higher role than required")
        void shouldReturnTrueWhenUserHasHigherRole() {
            // Given
            when(organizationMembershipService.getUserRole(USER_ID, ORG_ID))
                    .thenReturn(OrganizationRole.OWNER);

            // When
            boolean result = organizationSecurityService.hasRole(USER_ID, ORG_ID, OrganizationRole.ADMIN);

            // Then
            assertTrue(result);
            verify(organizationMembershipService).getUserRole(USER_ID, ORG_ID);
        }

        @Test
        @DisplayName("Should return false when user has lower role than required")
        void shouldReturnFalseWhenUserHasLowerRole() {
            // Given
            when(organizationMembershipService.getUserRole(USER_ID, ORG_ID))
                    .thenReturn(OrganizationRole.MEMBER);

            // When
            boolean result = organizationSecurityService.hasRole(USER_ID, ORG_ID, OrganizationRole.ADMIN);

            // Then
            assertFalse(result);
            verify(organizationMembershipService).getUserRole(USER_ID, ORG_ID);
        }

        @Test
        @DisplayName("Should return false when user has no role")
        void shouldReturnFalseWhenUserHasNoRole() {
            // Given
            when(organizationMembershipService.getUserRole(USER_ID, ORG_ID))
                    .thenReturn(null);

            // When
            boolean result = organizationSecurityService.hasRole(USER_ID, ORG_ID, OrganizationRole.MEMBER);

            // Then
            assertFalse(result);
            verify(organizationMembershipService).getUserRole(USER_ID, ORG_ID);
        }

        @Test
        @DisplayName("Should return false when required role is null")
        void shouldReturnFalseWhenRequiredRoleIsNull() {
            // Given
            when(organizationMembershipService.getUserRole(USER_ID, ORG_ID))
                    .thenReturn(OrganizationRole.MEMBER);

            // When
            boolean result = organizationSecurityService.hasRole(USER_ID, ORG_ID, null);

            // Then
            assertFalse(result);
            verify(organizationMembershipService).getUserRole(USER_ID, ORG_ID);
        }

        @Test
        @DisplayName("Should test complete role hierarchy")
        void shouldTestCompleteRoleHierarchy() {
            // Test OWNER can access all roles
            when(organizationMembershipService.getUserRole(USER_ID, ORG_ID))
                    .thenReturn(OrganizationRole.OWNER);

            assertTrue(organizationSecurityService.hasRole(USER_ID, ORG_ID, OrganizationRole.VIEWER));
            assertTrue(organizationSecurityService.hasRole(USER_ID, ORG_ID, OrganizationRole.MEMBER));
            assertTrue(organizationSecurityService.hasRole(USER_ID, ORG_ID, OrganizationRole.ADMIN));
            assertTrue(organizationSecurityService.hasRole(USER_ID, ORG_ID, OrganizationRole.OWNER));

            // Test VIEWER can only access VIEWER
            when(organizationMembershipService.getUserRole(USER_ID, ORG_ID))
                    .thenReturn(OrganizationRole.VIEWER);

            assertTrue(organizationSecurityService.hasRole(USER_ID, ORG_ID, OrganizationRole.VIEWER));
            assertFalse(organizationSecurityService.hasRole(USER_ID, ORG_ID, OrganizationRole.MEMBER));
            assertFalse(organizationSecurityService.hasRole(USER_ID, ORG_ID, OrganizationRole.ADMIN));
            assertFalse(organizationSecurityService.hasRole(USER_ID, ORG_ID, OrganizationRole.OWNER));
        }

        @Test
        @DisplayName("Should return false when exception is thrown")
        void shouldReturnFalseWhenExceptionThrown() {
            // Given
            when(organizationMembershipService.getUserRole(USER_ID, ORG_ID))
                    .thenThrow(new RuntimeException("Service error"));

            // When
            boolean result = organizationSecurityService.hasRole(USER_ID, ORG_ID, OrganizationRole.MEMBER);

            // Then
            assertFalse(result);
            verify(organizationMembershipService).getUserRole(USER_ID, ORG_ID);
        }
    }

    @Nested
    @DisplayName("Permission-based methods tests")
    class PermissionMethodsTests {

        @Test
        @DisplayName("canInviteMembers should return true when user is admin")
        void canInviteMembersShouldReturnTrueWhenUserIsAdmin() {
            // Given
            when(organizationMembershipService.isUserAdmin(USER_ID, ORG_ID)).thenReturn(true);

            // When
            boolean result = organizationSecurityService.canInviteMembers(USER_ID, ORG_ID);

            // Then
            assertTrue(result);
            verify(organizationMembershipService).isUserAdmin(USER_ID, ORG_ID);
        }

        @Test
        @DisplayName("canInviteMembers should return false when user is not admin")
        void canInviteMembersShouldReturnFalseWhenUserIsNotAdmin() {
            // Given
            when(organizationMembershipService.isUserAdmin(USER_ID, ORG_ID)).thenReturn(false);

            // When
            boolean result = organizationSecurityService.canInviteMembers(USER_ID, ORG_ID);

            // Then
            assertFalse(result);
            verify(organizationMembershipService).isUserAdmin(USER_ID, ORG_ID);
        }

        @Test
        @DisplayName("canManageMembers should return true when user is owner")
        void canManageMembersShouldReturnTrueWhenUserIsOwner() {
            // Given
            when(organizationMembershipService.isUserAdmin(USER_ID, ORG_ID)).thenReturn(false);
            when(organizationMembershipService.isUserOwner(USER_ID, ORG_ID)).thenReturn(true);

            // When
            boolean result = organizationSecurityService.canManageMembers(USER_ID, ORG_ID);

            // Then
            assertTrue(result);
            verify(organizationMembershipService).isUserAdmin(USER_ID, ORG_ID);
            verify(organizationMembershipService).isUserOwner(USER_ID, ORG_ID);
        }

        @Test
        @DisplayName("canManageMembers should return true when user is admin but not owner")
        void canManageMembersShouldReturnTrueWhenUserIsAdminButNotOwner() {
            // Given
            when(organizationMembershipService.isUserAdmin(USER_ID, ORG_ID)).thenReturn(true);

            // When
            boolean result = organizationSecurityService.canManageMembers(USER_ID, ORG_ID);

            // Then
            assertTrue(result);
            verify(organizationMembershipService).isUserAdmin(USER_ID, ORG_ID);
            verify(organizationMembershipService, never()).isUserOwner(USER_ID, ORG_ID);
        }

        @Test
        @DisplayName("canManageMembers should return false when user is not admin")
        void canManageMembersShouldReturnFalseWhenUserIsNotAdmin() {
            // Given
            when(organizationMembershipService.isUserAdmin(USER_ID, ORG_ID)).thenReturn(false);
            when(organizationMembershipService.isUserOwner(USER_ID, ORG_ID)).thenReturn(false);

            // When
            boolean result = organizationSecurityService.canManageMembers(USER_ID, ORG_ID);

            // Then
            assertFalse(result);
            verify(organizationMembershipService).isUserAdmin(USER_ID, ORG_ID);
            verify(organizationMembershipService).isUserOwner(any(), any());
        }

        @Test
        @DisplayName("canRemoveMember should return true when user removes themselves")
        void canRemoveMemberShouldReturnTrueWhenUserRemovesThemselves() {
            // Given
            when(organizationService.isUserMember(USER_ID, ORG_ID)).thenReturn(true);

            // When
            boolean result = organizationSecurityService.canRemoveMember(USER_ID, ORG_ID, USER_ID);

            // Then
            assertTrue(result);
            verify(organizationService).isUserMember(USER_ID, ORG_ID);
        }

        @Test
        @DisplayName("canRemoveMember should return false when user removes themselves but is not member")
        void canRemoveMemberShouldReturnFalseWhenUserRemovesThemselvesButNotMember() {
            // Given
            when(organizationService.isUserMember(USER_ID, ORG_ID)).thenReturn(false);

            // When
            boolean result = organizationSecurityService.canRemoveMember(USER_ID, ORG_ID, USER_ID);

            // Then
            assertFalse(result);
            verify(organizationService).isUserMember(USER_ID, ORG_ID);
        }

        @Test
        @DisplayName("canRemoveMember should return true when admin removes other member")
        void canRemoveMemberShouldReturnTrueWhenAdminRemovesOtherMember() {
            // Given
            when(organizationMembershipService.isUserAdmin(USER_ID, ORG_ID)).thenReturn(true);

            // When
            boolean result = organizationSecurityService.canRemoveMember(USER_ID, ORG_ID, TARGET_USER_ID);

            // Then
            assertTrue(result);
            verify(organizationMembershipService).isUserAdmin(USER_ID, ORG_ID);
        }

        @Test
        @DisplayName("canRemoveMember should return false when non-admin tries to remove other member")
        void canRemoveMemberShouldReturnFalseWhenNonAdminTriesToRemoveOtherMember() {
            // Given
            when(organizationMembershipService.isUserAdmin(USER_ID, ORG_ID)).thenReturn(false);

            // When
            boolean result = organizationSecurityService.canRemoveMember(USER_ID, ORG_ID, TARGET_USER_ID);

            // Then
            assertFalse(result);
            verify(organizationMembershipService).isUserAdmin(USER_ID, ORG_ID);
        }

        @Test
        @DisplayName("canViewOrganization should delegate to isMember")
        void canViewOrganizationShouldDelegateToIsMember() {
            // Given
            when(organizationService.isUserMember(USER_ID, ORG_ID)).thenReturn(true);

            // When
            boolean result = organizationSecurityService.canViewOrganization(USER_ID, ORG_ID);

            // Then
            assertTrue(result);
            verify(organizationService).isUserMember(USER_ID, ORG_ID);
        }

        @Test
        @DisplayName("canViewStats should delegate to isAdmin")
        void canViewStatsShouldDelegateToIsAdmin() {
            // Given
            when(organizationMembershipService.isUserAdmin(USER_ID, ORG_ID)).thenReturn(true);

            // When
            boolean result = organizationSecurityService.canViewStats(USER_ID, ORG_ID);

            // Then
            assertTrue(result);
            verify(organizationMembershipService).isUserAdmin(USER_ID, ORG_ID);
        }

        @Test
        @DisplayName("canArchiveOrganization should delegate to isOwner")
        void canArchiveOrganizationShouldDelegateToIsOwner() {
            // Given
            when(organizationMembershipService.isUserOwner(USER_ID, ORG_ID)).thenReturn(true);

            // When
            boolean result = organizationSecurityService.canArchiveOrganization(USER_ID, ORG_ID);

            // Then
            assertTrue(result);
            verify(organizationMembershipService).isUserOwner(USER_ID, ORG_ID);
        }
    }

    @Nested
    @DisplayName("Edge cases and null safety tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null userId gracefully")
        void shouldHandleNullUserIdGracefully() {
            // When & Then
            assertFalse(organizationSecurityService.isMember(null, ORG_ID));
            assertFalse(organizationSecurityService.isAdmin(null, ORG_ID));
            assertFalse(organizationSecurityService.isOwner(null, ORG_ID));
            assertFalse(organizationSecurityService.hasRole(null, ORG_ID, OrganizationRole.MEMBER));
        }

        @Test
        @DisplayName("Should handle null organizationId gracefully")
        void shouldHandleNullOrganizationIdGracefully() {
            // When & Then
            assertFalse(organizationSecurityService.isMember(USER_ID, null));
            assertFalse(organizationSecurityService.isAdmin(USER_ID, null));
            assertFalse(organizationSecurityService.isOwner(USER_ID, null));
            assertFalse(organizationSecurityService.hasRole(USER_ID, null, OrganizationRole.MEMBER));
        }

        @Test
        @DisplayName("Should handle empty string organizationId gracefully")
        void shouldHandleEmptyStringOrganizationIdGracefully() {
            // Given
            String emptyOrgId = "";
            when(organizationService.isUserMember(USER_ID, emptyOrgId)).thenReturn(false);
            when(organizationMembershipService.isUserAdmin(USER_ID, emptyOrgId)).thenReturn(false);
            when(organizationMembershipService.isUserOwner(USER_ID, emptyOrgId)).thenReturn(false);
            when(organizationMembershipService.getUserRole(USER_ID, emptyOrgId)).thenReturn(null);

            // When & Then
            assertFalse(organizationSecurityService.isMember(USER_ID, emptyOrgId));
            assertFalse(organizationSecurityService.isAdmin(USER_ID, emptyOrgId));
            assertFalse(organizationSecurityService.isOwner(USER_ID, emptyOrgId));
            assertFalse(organizationSecurityService.hasRole(USER_ID, emptyOrgId, OrganizationRole.MEMBER));
        }
    }
}
