package com.lagab.eventz.app.org.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lagab.eventz.app.domain.org.dto.OrganizationDto;
import com.lagab.eventz.app.domain.org.mapper.OrganizationMapper;
import com.lagab.eventz.app.domain.org.model.Organization;
import com.lagab.eventz.app.domain.org.model.OrganizationMembership;
import com.lagab.eventz.app.domain.org.model.OrganizationRole;
import com.lagab.eventz.app.domain.org.repository.OrganizationMembershipRepository;
import com.lagab.eventz.app.domain.org.repository.OrganizationRepository;
import com.lagab.eventz.app.domain.org.service.OrganizationService;
import com.lagab.eventz.app.domain.user.model.User;

import jakarta.persistence.EntityNotFoundException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationService Tests")
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationMembershipRepository membershipRepository;

    @Mock
    private OrganizationMapper organizationMapper;

    @InjectMocks
    private OrganizationService organizationService;

    private User testUser;
    private Organization testOrganization;
    private OrganizationDto testOrganizationDto;
    private OrganizationMembership testMembership;

    @BeforeEach
    void setUp() {
        // Setup test data
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        testOrganization = new Organization();
        testOrganization.setId("org-123");
        testOrganization.setName("Test Organization");
        testOrganization.setSlug("test-org");
        testOrganization.setEmail("emaim@test.com");

        testOrganizationDto = new OrganizationDto(null, "org-123", "test-org", "emaim@test.com", null, null);

        testMembership = new OrganizationMembership();
        testMembership.setUser(testUser);
        testMembership.setOrganization(testOrganization);
        testMembership.setRole(OrganizationRole.OWNER);
    }

    @Nested
    @DisplayName("Create Organization Tests")
    class CreateOrganizationTests {

        @Test
        @DisplayName("Should create organization successfully and assign creator as owner")
        void shouldCreateOrganizationSuccessfully() {
            // Given
            when(organizationMapper.toEntity(testOrganizationDto)).thenReturn(testOrganization);
            when(organizationRepository.save(testOrganization)).thenReturn(testOrganization);
            when(membershipRepository.save(any(OrganizationMembership.class))).thenReturn(testMembership);
            when(organizationMapper.toDto(testOrganization)).thenReturn(testOrganizationDto);

            // When
            OrganizationDto result = organizationService.createOrganization(testOrganizationDto, testUser);

            // Then
            assertNotNull(result);
            assertEquals(testOrganizationDto.id(), result.id());
            assertEquals(testOrganizationDto.name(), result.name());

            verify(organizationMapper).toEntity(testOrganizationDto);
            verify(organizationRepository).save(testOrganization);
            verify(membershipRepository).save(argThat(membership ->
                    membership.getUser().equals(testUser) &&
                            membership.getOrganization().equals(testOrganization) &&
                            membership.getRole().equals(OrganizationRole.OWNER)
            ));
            verify(organizationMapper).toDto(testOrganization);
        }

        @Test
        @DisplayName("Should handle repository exception during organization creation")
        void shouldHandleRepositoryExceptionDuringCreation() {
            // Given
            when(organizationMapper.toEntity(testOrganizationDto)).thenReturn(testOrganization);
            when(organizationRepository.save(testOrganization)).thenThrow(new RuntimeException("Database error"));

            // When & Then
            assertThrows(RuntimeException.class, () ->
                    organizationService.createOrganization(testOrganizationDto, testUser)
            );

            verify(organizationRepository).save(testOrganization);
            verify(membershipRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get User Organizations Tests")
    class GetUserOrganizationsTests {

        @Test
        @DisplayName("Should return list of user organizations")
        void shouldReturnUserOrganizations() {
            // Given
            Long userId = 1L;
            List<Organization> organizations = Arrays.asList(testOrganization);
            List<OrganizationDto> organizationDtos = Arrays.asList(testOrganizationDto);

            when(organizationRepository.findByUserId(userId)).thenReturn(organizations);
            when(organizationMapper.toDtos(organizations)).thenReturn(organizationDtos);

            // When
            List<OrganizationDto> result = organizationService.getUserOrganizations(userId);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(testOrganizationDto.id(), result.get(0).id());

            verify(organizationRepository).findByUserId(userId);
            verify(organizationMapper).toDtos(organizations);
        }

        @Test
        @DisplayName("Should return empty list when user has no organizations")
        void shouldReturnEmptyListWhenUserHasNoOrganizations() {
            // Given
            Long userId = 1L;
            when(organizationRepository.findByUserId(userId)).thenReturn(Arrays.asList());
            when(organizationMapper.toDtos(Arrays.asList())).thenReturn(Arrays.asList());

            // When
            List<OrganizationDto> result = organizationService.getUserOrganizations(userId);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Get Organization Tests")
    class GetOrganizationTests {

        @Test
        @DisplayName("Should return organization by ID")
        void shouldReturnOrganizationById() {
            // Given
            String orgId = "org-123";
            when(organizationRepository.findById(orgId)).thenReturn(Optional.of(testOrganization));
            when(organizationMapper.toDto(testOrganization)).thenReturn(testOrganizationDto);

            // When
            OrganizationDto result = organizationService.getOrganization(orgId);

            // Then
            assertNotNull(result);
            assertEquals(testOrganizationDto.id(), result.id());

            verify(organizationRepository).findById(orgId);
            verify(organizationMapper).toDto(testOrganization);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when organization not found")
        void shouldThrowEntityNotFoundExceptionWhenOrganizationNotFound() {
            // Given
            String orgId = "non-existent";
            when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

            // When & Then
            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () ->
                    organizationService.getOrganization(orgId)
            );

            assertEquals("Organization not found with id: " + orgId, exception.getMessage());
            verify(organizationRepository).findById(orgId);
            verify(organizationMapper, never()).toDto(any());
        }
    }

    @Nested
    @DisplayName("Update Organization Tests")
    class UpdateOrganizationTests {

        @Test
        @DisplayName("Should update organization when user is admin")
        void shouldUpdateOrganizationWhenUserIsAdmin() {
            // Given
            String orgId = "org-123";
            Long userId = 1L;
            OrganizationDto updateDto = new OrganizationDto("org-123", "Updated Organization", "test-org", "emaim@test.com", null, null);

            when(organizationRepository.findById(orgId)).thenReturn(Optional.of(testOrganization));
            when(membershipRepository.existsByUserIdAndOrganizationIdAndRoleIn(userId, orgId,
                    List.of(OrganizationRole.ADMIN.name(), OrganizationRole.OWNER.name()))).thenReturn(true);
            when(organizationRepository.save(testOrganization)).thenReturn(testOrganization);
            when(organizationMapper.toDto(testOrganization)).thenReturn(testOrganizationDto);

            // When
            OrganizationDto result = organizationService.updateOrganization(orgId, updateDto, userId);

            // Then
            assertNotNull(result);
            assertEquals(testOrganizationDto.id(), result.id());

            verify(organizationRepository).findById(orgId);
            verify(membershipRepository).existsByUserIdAndOrganizationIdAndRoleIn(userId, orgId,
                    List.of(OrganizationRole.ADMIN.name(), OrganizationRole.OWNER.name()));
            verify(organizationMapper).updateEntityFromDto(updateDto, testOrganization);
            verify(organizationRepository).save(testOrganization);
            verify(organizationMapper).toDto(testOrganization);
        }

        @Test
        @DisplayName("Should throw SecurityException when user is not admin")
        void shouldThrowSecurityExceptionWhenUserIsNotAdmin() {
            // Given
            String orgId = "org-123";
            Long userId = 2L;
            OrganizationDto updateDto = new OrganizationDto("org-123", "Updated Organization", "test-org", "emaim@test.com", null, null);

            when(organizationRepository.findById(orgId)).thenReturn(Optional.of(testOrganization));
            when(membershipRepository.existsByUserIdAndOrganizationIdAndRoleIn(userId, orgId,
                    List.of(OrganizationRole.ADMIN.name(), OrganizationRole.OWNER.name()))).thenReturn(false);

            // When & Then
            SecurityException exception = assertThrows(SecurityException.class, () ->
                    organizationService.updateOrganization(orgId, updateDto, userId)
            );

            assertEquals("Not authorized to perform this action", exception.getMessage());
            verify(organizationMapper, never()).updateEntityFromDto(any(), any());
            verify(organizationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when organization not found for update")
        void shouldThrowEntityNotFoundExceptionWhenOrganizationNotFoundForUpdate() {
            // Given
            String orgId = "non-existent";
            Long userId = 1L;
            OrganizationDto updateDto = new OrganizationDto("non-existent", "Updated Organization", "test-org", "emaim@test.com", null, null);

            when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

            // When & Then
            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () ->
                    organizationService.updateOrganization(orgId, updateDto, userId)
            );

            assertEquals("Organization not found with id: " + orgId, exception.getMessage());
            verify(membershipRepository, never()).existsByUserIdAndOrganizationIdAndRoleIn(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Delete Organization Tests")
    class DeleteOrganizationTests {

        @Test
        @DisplayName("Should delete organization when user is admin")
        void shouldDeleteOrganizationWhenUserIsAdmin() {
            // Given
            String orgId = "org-123";
            Long userId = 1L;

            when(organizationRepository.findById(orgId)).thenReturn(Optional.of(testOrganization));
            when(membershipRepository.existsByUserIdAndOrganizationIdAndRoleIn(userId, orgId,
                    List.of(OrganizationRole.ADMIN.name(), OrganizationRole.OWNER.name()))).thenReturn(true);

            // When
            organizationService.deleteOrganization(orgId, userId);

            // Then
            verify(organizationRepository).findById(orgId);
            verify(membershipRepository).existsByUserIdAndOrganizationIdAndRoleIn(userId, orgId,
                    List.of(OrganizationRole.ADMIN.name(), OrganizationRole.OWNER.name()));
            verify(membershipRepository).deleteByOrganizationId(orgId);
            verify(organizationRepository).delete(testOrganization);
        }

        @Test
        @DisplayName("Should throw SecurityException when user is not admin for deletion")
        void shouldThrowSecurityExceptionWhenUserIsNotAdminForDeletion() {
            // Given
            String orgId = "org-123";
            Long userId = 2L;

            when(organizationRepository.findById(orgId)).thenReturn(Optional.of(testOrganization));
            when(membershipRepository.existsByUserIdAndOrganizationIdAndRoleIn(userId, orgId,
                    List.of(OrganizationRole.ADMIN.name(), OrganizationRole.OWNER.name()))).thenReturn(false);

            // When & Then
            SecurityException exception = assertThrows(SecurityException.class, () ->
                    organizationService.deleteOrganization(orgId, userId)
            );

            assertEquals("Not authorized to perform this action", exception.getMessage());
            verify(membershipRepository, never()).deleteByOrganizationId(any());
            verify(organizationRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Admin Verification Tests")
    class AdminVerificationTests {

        @Test
        @DisplayName("Should pass when user is admin")
        void shouldPassWhenUserIsAdmin() {
            // Given
            Long userId = 1L;
            String orgId = "org-123";

            when(membershipRepository.existsByUserIdAndOrganizationIdAndRoleIn(userId, orgId,
                    List.of(OrganizationRole.ADMIN.name(), OrganizationRole.OWNER.name()))).thenReturn(true);

            // When & Then
            assertDoesNotThrow(() -> organizationService.ensureUserIsAdmin(userId, orgId));

            verify(membershipRepository).existsByUserIdAndOrganizationIdAndRoleIn(userId, orgId,
                    List.of(OrganizationRole.ADMIN.name(), OrganizationRole.OWNER.name()));
        }

        @Test
        @DisplayName("Should throw SecurityException when user is not admin")
        void shouldThrowSecurityExceptionWhenUserIsNotAdmin() {
            // Given
            Long userId = 2L;
            String orgId = "org-123";

            when(membershipRepository.existsByUserIdAndOrganizationIdAndRoleIn(userId, orgId,
                    List.of(OrganizationRole.ADMIN.name(), OrganizationRole.OWNER.name()))).thenReturn(false);

            // When & Then
            SecurityException exception = assertThrows(SecurityException.class, () ->
                    organizationService.ensureUserIsAdmin(userId, orgId)
            );

            assertEquals("Not authorized to perform this action", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Membership Check Tests")
    class MembershipCheckTests {

        @Test
        @DisplayName("Should return true when user is member")
        void shouldReturnTrueWhenUserIsMember() {
            // Given
            Long userId = 1L;
            String orgId = "org-123";

            when(membershipRepository.existsByUserIdAndOrganizationId(userId, orgId)).thenReturn(true);

            // When
            boolean result = organizationService.isUserMember(userId, orgId);

            // Then
            assertTrue(result);
            verify(membershipRepository).existsByUserIdAndOrganizationId(userId, orgId);
        }

        @Test
        @DisplayName("Should return false when user is not member")
        void shouldReturnFalseWhenUserIsNotMember() {
            // Given
            Long userId = 2L;
            String orgId = "org-123";

            when(membershipRepository.existsByUserIdAndOrganizationId(userId, orgId)).thenReturn(false);

            // When
            boolean result = organizationService.isUserMember(userId, orgId);

            // Then
            assertFalse(result);
            verify(membershipRepository).existsByUserIdAndOrganizationId(userId, orgId);
        }
    }

    @Nested
    @DisplayName("Slug Validation Tests")
    class SlugValidationTests {

        @Test
        @DisplayName("Should return true when slug is taken")
        void shouldReturnTrueWhenSlugIsTaken() {
            // Given
            String slug = "existing-slug";
            when(organizationRepository.existsBySlug(slug)).thenReturn(true);

            // When
            boolean result = organizationService.isSlugTaken(slug);

            // Then
            assertTrue(result);
            verify(organizationRepository).existsBySlug(slug);
        }

        @Test
        @DisplayName("Should return false when slug is available")
        void shouldReturnFalseWhenSlugIsAvailable() {
            // Given
            String slug = "available-slug";
            when(organizationRepository.existsBySlug(slug)).thenReturn(false);

            // When
            boolean result = organizationService.isSlugTaken(slug);

            // Then
            assertFalse(result);
            verify(organizationRepository).existsBySlug(slug);
        }
    }
}
