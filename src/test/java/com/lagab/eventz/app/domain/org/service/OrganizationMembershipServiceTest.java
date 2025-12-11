package com.lagab.eventz.app.domain.org.service;

import java.time.LocalDateTime;
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

import com.lagab.eventz.app.common.exception.BusinessException;
import com.lagab.eventz.app.domain.auth.service.EmailService;
import com.lagab.eventz.app.domain.org.dto.OrganizationDto;
import com.lagab.eventz.app.domain.org.dto.OrganizationMembershipDto;
import com.lagab.eventz.app.domain.org.dto.UserInfoDto;
import com.lagab.eventz.app.domain.org.dto.invitation.InvitationCreateDto;
import com.lagab.eventz.app.domain.org.dto.invitation.InvitationResponseDto;
import com.lagab.eventz.app.domain.org.dto.invitation.MembershipInviteDto;
import com.lagab.eventz.app.domain.org.mapper.InvitationMapper;
import com.lagab.eventz.app.domain.org.mapper.OrganizationMembershipMapper;
import com.lagab.eventz.app.domain.org.model.Invitation;
import com.lagab.eventz.app.domain.org.model.Organization;
import com.lagab.eventz.app.domain.org.model.OrganizationMembership;
import com.lagab.eventz.app.domain.org.model.OrganizationRole;
import com.lagab.eventz.app.domain.org.repository.InvitationRepository;
import com.lagab.eventz.app.domain.org.repository.OrganizationMembershipRepository;
import com.lagab.eventz.app.domain.user.model.User;
import com.lagab.eventz.app.domain.user.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationMembershipService Tests")
class OrganizationMembershipServiceTest {

    @Mock
    private OrganizationService organizationService;

    @Mock
    private EmailService emailService;

    @Mock
    private OrganizationMembershipRepository membershipRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private OrganizationMembershipMapper membershipMapper;

    @Mock
    private InvitationMapper invitationMapper;

    @InjectMocks
    private OrganizationMembershipService membershipService;

    private User testUser;
    private User inviterUser;
    private Organization testOrganization;
    private OrganizationMembership testMembership;
    private Invitation testInvitation;
    private OrganizationDto testOrganizationDto;
    private OrganizationMembershipDto testMembershipDto;
    private UserInfoDto testUserInfoDto;
    private InvitationResponseDto testInvitationResponseDto;

    @BeforeEach
    void setUp() {
        // Setup test users
        testUser = User.builder()
                       .id(1L)
                       .email("test@example.com")
                       .firstName("John")
                       .lastName("Doe")
                       .build();

        inviterUser = User.builder()
                          .id(2L)
                          .email("inviter@example.com")
                          .firstName("Jane")
                          .lastName("Smith")
                          .build();

        // Setup test organization
        testOrganization = new Organization();
        testOrganization.setId("org-123");
        testOrganization.setName("Test Organization");
        testOrganization.setSlug("test-org");

        testOrganizationDto = new OrganizationDto(
                "org-123",
                "Test Organization",
                "test-org",
                "org@example.com",
                "logo.png",
                null
        );

        // Setup test membership
        testMembership = new OrganizationMembership();
        testMembership.setId(1L);
        testMembership.setUser(testUser);
        testMembership.setOrganization(testOrganization);
        testMembership.setRole(OrganizationRole.MEMBER);

        testUserInfoDto = new UserInfoDto(1L, "test@example.com", "John", "Doe");

        testMembershipDto = new OrganizationMembershipDto(
                1L,
                testOrganizationDto,
                OrganizationRole.MEMBER,
                testUserInfoDto
        );

        // Setup test invitation
        testInvitation = new Invitation();
        testInvitation.setId(1L);
        testInvitation.setEmail("invite@example.com");
        testInvitation.setRole(OrganizationRole.MEMBER);
        testInvitation.setToken("secure-token-123");
        testInvitation.setExpiresAt(LocalDateTime.now().plusDays(7));
        testInvitation.setCreatedAt(LocalDateTime.now());
        testInvitation.setInvitedBy(inviterUser);
        testInvitation.setOrganization(testOrganization);

        testInvitationResponseDto = new InvitationResponseDto(
                1L,
                "invite@example.com",
                OrganizationRole.MEMBER,
                "secure-token-123",
                LocalDateTime.now().plusDays(7),
                LocalDateTime.now(),
                new UserInfoDto(2L, "inviter@example.com", "Jane", "Smith"),
                testOrganizationDto
        );
    }

    @Nested
    @DisplayName("Invite Member Tests")
    class InviteMemberTests {

        @Test
        @DisplayName("Should invite existing user successfully")
        void shouldInviteExistingUserSuccessfully() {
            // Given
            MembershipInviteDto inviteDto = new MembershipInviteDto(
                    1L, "test@example.com", "org-123", OrganizationRole.MEMBER
            );
            Long inviterId = 2L;

            when(membershipRepository.existsByUserIdAndOrganizationId(1L, "org-123")).thenReturn(false);
            when(membershipMapper.inviteDtoToEntity(inviteDto)).thenReturn(testMembership);
            when(membershipRepository.save(testMembership)).thenReturn(testMembership);
            when(membershipMapper.toDto(testMembership)).thenReturn(testMembershipDto);

            // When
            OrganizationMembershipDto result = membershipService.inviteMember(inviteDto, inviterId);

            // Then
            assertNotNull(result);
            assertEquals(testMembershipDto.id(), result.id());
            assertEquals(testMembershipDto.role(), result.role());

            verify(organizationService).ensureUserIsAdmin(inviterId, "org-123");
            verify(membershipRepository).existsByUserIdAndOrganizationId(1L, "org-123");
            verify(membershipRepository).save(testMembership);
        }

        @Test
        @DisplayName("Should send invitation when user does not exist")
        void shouldSendInvitationWhenUserDoesNotExist() {
            // Given
            MembershipInviteDto inviteDto = new MembershipInviteDto(
                    null, "newuser@example.com", "org-123", OrganizationRole.MEMBER
            );
            Long inviterId = 2L;

            when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
            when(organizationService.getOrganization("org-123")).thenReturn(testOrganizationDto);
            when(userRepository.findById(inviterId)).thenReturn(Optional.of(inviterUser));
            when(invitationRepository.existsByEmailAndOrganizationId("newuser@example.com", "org-123")).thenReturn(false);
            when(invitationMapper.createDtoToEntity(any(InvitationCreateDto.class))).thenReturn(testInvitation);
            when(invitationRepository.save(testInvitation)).thenReturn(testInvitation);
            when(invitationMapper.entityToResponseDto(testInvitation)).thenReturn(testInvitationResponseDto);

            // When
            OrganizationMembershipDto result = membershipService.inviteMember(inviteDto, inviterId);

            // Then
            assertNull(result);

            verify(organizationService).ensureUserIsAdmin(inviterId, "org-123");
            verify(userRepository).findByEmail("newuser@example.com");
            verify(emailService).sendOrganizationInvitation(eq(testOrganizationDto), eq(inviterUser), anyString(), eq("newuser@example.com"));
        }

        @Test
        @DisplayName("Should throw SecurityException when inviter is not admin")
        void shouldThrowSecurityExceptionWhenInviterIsNotAdmin() {
            // Given
            MembershipInviteDto inviteDto = new MembershipInviteDto(
                    1L, "test@example.com", "org-123", OrganizationRole.MEMBER
            );
            Long inviterId = 3L;

            doThrow(new SecurityException("Not authorized to perform this action"))
                    .when(organizationService).ensureUserIsAdmin(inviterId, "org-123");

            // When & Then
            SecurityException exception = assertThrows(SecurityException.class, () ->
                    membershipService.inviteMember(inviteDto, inviterId)
            );

            assertEquals("Not authorized to perform this action", exception.getMessage());
            verify(membershipRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when user is already member")
        void shouldThrowIllegalArgumentExceptionWhenUserIsAlreadyMember() {
            // Given
            MembershipInviteDto inviteDto = new MembershipInviteDto(
                    1L, "test@example.com", "org-123", OrganizationRole.MEMBER
            );
            Long inviterId = 2L;

            when(membershipRepository.existsByUserIdAndOrganizationId(1L, "org-123")).thenReturn(true);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    membershipService.inviteMember(inviteDto, inviterId)
            );

            assertEquals("User is already a member of this organization", exception.getMessage());
            verify(membershipRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should find user by email when userId is null")
        void shouldFindUserByEmailWhenUserIdIsNull() {
            // Given
            MembershipInviteDto inviteDto = new MembershipInviteDto(
                    null, "test@example.com", "org-123", OrganizationRole.MEMBER
            );
            Long inviterId = 2L;

            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(membershipRepository.existsByUserIdAndOrganizationId(1L, "org-123")).thenReturn(false);
            when(membershipMapper.inviteDtoToEntity(any(MembershipInviteDto.class))).thenReturn(testMembership);
            when(membershipRepository.save(testMembership)).thenReturn(testMembership);
            when(membershipMapper.toDto(testMembership)).thenReturn(testMembershipDto);

            // When
            OrganizationMembershipDto result = membershipService.inviteMember(inviteDto, inviterId);

            // Then
            assertNotNull(result);
            verify(userRepository).findByEmail("test@example.com");
            verify(membershipRepository).existsByUserIdAndOrganizationId(1L, "org-123");
        }
    }

    @Nested
    @DisplayName("Get Membership Tests")
    class GetMembershipTests {

        @Test
        @DisplayName("Should return membership by ID")
        void shouldReturnMembershipById() {
            // Given
            Long membershipId = 1L;
            when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(testMembership));
            when(membershipMapper.toDto(testMembership)).thenReturn(testMembershipDto);

            // When
            OrganizationMembershipDto result = membershipService.getMembership(membershipId);

            // Then
            assertNotNull(result);
            assertEquals(testMembershipDto.id(), result.id());
            verify(membershipRepository).findById(membershipId);
            verify(membershipMapper).toDto(testMembership);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when membership not found")
        void shouldThrowEntityNotFoundExceptionWhenMembershipNotFound() {
            // Given
            Long membershipId = 999L;
            when(membershipRepository.findById(membershipId)).thenReturn(Optional.empty());

            // When & Then
            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () ->
                    membershipService.getMembership(membershipId)
            );

            assertEquals("Membership not found with id: " + membershipId, exception.getMessage());
            verify(membershipMapper, never()).toDto(any());
        }
    }

    @Nested
    @DisplayName("Update Member Role Tests")
    class UpdateMemberRoleTests {

        @Test
        @DisplayName("Should update member role successfully")
        void shouldUpdateMemberRoleSuccessfully() {
            // Given
            Long membershipId = 1L;
            OrganizationRole newRole = OrganizationRole.ADMIN;
            Long updaterId = 2L;

            when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(testMembership));
            when(membershipRepository.save(testMembership)).thenReturn(testMembership);
            when(membershipMapper.toDto(testMembership)).thenReturn(testMembershipDto);

            // When
            OrganizationMembershipDto result = membershipService.updateMemberRole(membershipId, newRole, updaterId);

            // Then
            assertNotNull(result);
            assertEquals(OrganizationRole.ADMIN, testMembership.getRole());

            verify(organizationService).ensureUserIsAdmin(updaterId, "org-123");
            verify(membershipRepository).save(testMembership);
        }

        @Test
        @DisplayName("Should prevent removing last admin role")
        void shouldPreventRemovingLastAdminRole() {
            // Given
            Long membershipId = 1L;
            OrganizationRole newRole = OrganizationRole.MEMBER;
            Long updaterId = 2L;

            testMembership.setRole(OrganizationRole.ADMIN);

            when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(testMembership));
            when(membershipRepository.countAdminsByOrganizationId("org-123")).thenReturn(1L);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    membershipService.updateMemberRole(membershipId, newRole, updaterId)
            );

            assertEquals("Cannot remove the last admin from organization", exception.getMessage());
            verify(membershipRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should allow role change when multiple admins exist")
        void shouldAllowRoleChangeWhenMultipleAdminsExist() {
            // Given
            Long membershipId = 1L;
            OrganizationRole newRole = OrganizationRole.MEMBER;
            Long updaterId = 2L;

            testMembership.setRole(OrganizationRole.ADMIN);

            when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(testMembership));
            when(membershipRepository.countAdminsByOrganizationId("org-123")).thenReturn(2L);
            when(membershipRepository.save(testMembership)).thenReturn(testMembership);
            when(membershipMapper.toDto(testMembership)).thenReturn(testMembershipDto);

            // When
            OrganizationMembershipDto result = membershipService.updateMemberRole(membershipId, newRole, updaterId);

            // Then
            assertNotNull(result);
            assertEquals(OrganizationRole.MEMBER, testMembership.getRole());
            verify(membershipRepository).save(testMembership);
        }
    }

    @Nested
    @DisplayName("Remove Member Tests")
    class RemoveMemberTests {

        @Test
        @DisplayName("Should remove member successfully")
        void shouldRemoveMemberSuccessfully() {
            // Given
            Long membershipId = 1L;
            Long removerId = 2L;

            when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(testMembership));

            // When
            membershipService.removeMember(membershipId, removerId);

            // Then
            verify(organizationService).ensureUserIsAdmin(removerId, "org-123");
            verify(membershipRepository).delete(testMembership);
        }

        @Test
        @DisplayName("Should prevent removing last admin")
        void shouldPreventRemovingLastAdmin() {
            // Given
            Long membershipId = 1L;
            Long removerId = 2L;

            testMembership.setRole(OrganizationRole.ADMIN);

            when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(testMembership));
            when(membershipRepository.countAdminsByOrganizationId("org-123")).thenReturn(1L);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    membershipService.removeMember(membershipId, removerId)
            );

            assertEquals("Cannot remove the last admin from organization", exception.getMessage());
            verify(membershipRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should allow removing admin when multiple admins exist")
        void shouldAllowRemovingAdminWhenMultipleAdminsExist() {
            // Given
            Long membershipId = 1L;
            Long removerId = 2L;

            testMembership.setRole(OrganizationRole.ADMIN);

            when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(testMembership));
            when(membershipRepository.countAdminsByOrganizationId("org-123")).thenReturn(2L);

            // When
            membershipService.removeMember(membershipId, removerId);

            // Then
            verify(membershipRepository).delete(testMembership);
        }
    }

    @Nested
    @DisplayName("Get Organization Members Tests")
    class GetOrganizationMembersTests {

        @Test
        @DisplayName("Should return organization members when user is member")
        void shouldReturnOrganizationMembersWhenUserIsMember() {
            // Given
            String organizationId = "org-123";
            Long userId = 1L;
            List<OrganizationMembership> memberships = Arrays.asList(testMembership);
            List<OrganizationMembershipDto> membershipDtos = Arrays.asList(testMembershipDto);

            when(organizationService.isUserMember(userId, organizationId)).thenReturn(true);
            when(membershipRepository.findMembersByOrganizationId(organizationId)).thenReturn(memberships);
            when(membershipMapper.entitiesToDtos(memberships)).thenReturn(membershipDtos);

            // When
            List<OrganizationMembershipDto> result = membershipService.getOrganizationMembers(organizationId, userId);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(testMembershipDto.id(), result.getFirst().id());

            verify(organizationService).isUserMember(userId, organizationId);
            verify(membershipRepository).findMembersByOrganizationId(organizationId);
        }

        @Test
        @DisplayName("Should throw SecurityException when user is not member")
        void shouldThrowSecurityExceptionWhenUserIsNotMember() {
            // Given
            String organizationId = "org-123";
            Long userId = 3L;

            when(organizationService.isUserMember(userId, organizationId)).thenReturn(false);

            // When & Then
            SecurityException exception = assertThrows(SecurityException.class, () ->
                    membershipService.getOrganizationMembers(organizationId, userId)
            );

            assertEquals("Not authorized to view organization members", exception.getMessage());
            verify(membershipRepository, never()).findMembersByOrganizationId(any());
        }
    }

    @Nested
    @DisplayName("Invite New User Tests")
    class InviteNewUserTests {

        @Test
        @DisplayName("Should create invitation for new user")
        void shouldCreateInvitationForNewUser() {
            // Given
            InvitationCreateDto invitationDto = new InvitationCreateDto(
                    "newuser@example.com", "org-123", OrganizationRole.MEMBER
            );

            when(invitationRepository.existsByEmailAndOrganizationId("newuser@example.com", "org-123")).thenReturn(false);
            when(invitationMapper.createDtoToEntity(invitationDto)).thenReturn(testInvitation);
            when(invitationRepository.save(any(Invitation.class))).thenReturn(testInvitation);
            when(invitationMapper.entityToResponseDto(testInvitation)).thenReturn(testInvitationResponseDto);
            when(organizationService.getOrganization("org-123")).thenReturn(testOrganizationDto);

            // When
            InvitationResponseDto result = membershipService.inviteNewUser(invitationDto, inviterUser);

            // Then
            assertNotNull(result);
            assertEquals(testInvitationResponseDto.email(), result.email());

            verify(invitationRepository).existsByEmailAndOrganizationId("newuser@example.com", "org-123");
            verify(invitationRepository).save(any(Invitation.class));
            verify(emailService).sendOrganizationInvitation(eq(testOrganizationDto), eq(inviterUser), anyString(), eq("newuser@example.com"));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when invitation already exists")
        void shouldThrowIllegalArgumentExceptionWhenInvitationAlreadyExists() {
            // Given
            InvitationCreateDto invitationDto = new InvitationCreateDto(
                    "existing@example.com", "org-123", OrganizationRole.MEMBER
            );

            when(invitationRepository.existsByEmailAndOrganizationId("existing@example.com", "org-123")).thenReturn(true);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class, () ->
                    membershipService.inviteNewUser(invitationDto, inviterUser)
            );

            assertEquals("Invitation already exists for this email and organization", exception.getMessage());
            verify(invitationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Accept Invitation Tests")
    class AcceptInvitationTests {

        @Test
        @DisplayName("Should accept invitation successfully")
        void shouldAcceptInvitationSuccessfully() {
            // Given
            String token = "valid-token";

            when(invitationRepository.findByTokenAndExpiresAtAfter(eq(token), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(testInvitation));
            when(membershipRepository.existsByUserIdAndOrganizationId(testUser.getId(), testInvitation.getOrganization().getId()))
                    .thenReturn(false);
            when(membershipMapper.inviteDtoToEntity(any(MembershipInviteDto.class))).thenReturn(testMembership);
            when(membershipRepository.save(testMembership)).thenReturn(testMembership);
            when(membershipMapper.toDto(testMembership)).thenReturn(testMembershipDto);

            // When
            OrganizationMembershipDto result = membershipService.acceptInvitation(token, testUser);

            // Then
            assertNotNull(result);
            assertEquals(testMembershipDto.id(), result.id());

            verify(invitationRepository).findByTokenAndExpiresAtAfter(eq(token), any(LocalDateTime.class));
            verify(invitationRepository).delete(testInvitation);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException for invalid token")
        void shouldThrowEntityNotFoundExceptionForInvalidToken() {
            // Given
            String token = "invalid-token";

            when(invitationRepository.findByTokenAndExpiresAtAfter(eq(token), any(LocalDateTime.class)))
                    .thenReturn(Optional.empty());

            // When & Then
            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () ->
                    membershipService.acceptInvitation(token, testUser)
            );

            assertEquals("Invalid or expired invitation", exception.getMessage());
            verify(invitationRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Pending Invitations Tests")
    class PendingInvitationsTests {

        @Test
        @DisplayName("Should return pending invitations when user is admin")
        void shouldReturnPendingInvitationsWhenUserIsAdmin() {
            // Given
            String organizationId = "org-123";
            Long userId = 2L;
            List<Invitation> invitations = Arrays.asList(testInvitation);
            List<InvitationResponseDto> invitationDtos = Arrays.asList(testInvitationResponseDto);

            when(invitationRepository.findByOrganizationId(organizationId)).thenReturn(invitations);
            when(invitationMapper.entitiesToResponseDtos(invitations)).thenReturn(invitationDtos);

            // When
            List<InvitationResponseDto> result = membershipService.getPendingInvitations(organizationId, userId);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(testInvitationResponseDto.email(), result.getFirst().email());

            verify(organizationService).ensureUserIsAdmin(userId, organizationId);
            verify(invitationRepository).findByOrganizationId(organizationId);
        }
    }

    @Nested
    @DisplayName("Cancel Invitation Tests")
    class CancelInvitationTests {

        @Test
        @DisplayName("Should cancel invitation when user is admin")
        void shouldCancelInvitationWhenUserIsAdmin() {
            // Given
            Long invitationId = 1L;
            Long userId = 2L;

            when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(testInvitation));

            // When
            membershipService.cancelInvitation(invitationId, userId);

            // Then
            verify(organizationService).ensureUserIsAdmin(userId, testInvitation.getOrganization().getId());
            verify(invitationRepository).delete(testInvitation);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when invitation not found")
        void shouldThrowEntityNotFoundExceptionWhenInvitationNotFound() {
            // Given
            Long invitationId = 999L;
            Long userId = 2L;

            when(invitationRepository.findById(invitationId)).thenReturn(Optional.empty());

            // When & Then
            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () ->
                    membershipService.cancelInvitation(invitationId, userId)
            );

            assertEquals("Invitation not found", exception.getMessage());
            verify(invitationRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Role Check Tests")
    class RoleCheckTests {

        @Test
        @DisplayName("Should return true when user is admin")
        void shouldReturnTrueWhenUserIsAdmin() {
            // Given
            Long userId = 1L;
            String organizationId = "org-123";
            testMembership.setRole(OrganizationRole.ADMIN);

            when(membershipRepository.findByUserIdAndOrganizationId(userId, organizationId))
                    .thenReturn(Optional.of(testMembership));

            // When
            boolean result = membershipService.isUserAdmin(userId, organizationId);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return true when user is owner")
        void shouldReturnTrueWhenUserIsOwner() {
            // Given
            Long userId = 1L;
            String organizationId = "org-123";
            testMembership.setRole(OrganizationRole.OWNER);

            when(membershipRepository.findByUserIdAndOrganizationId(userId, organizationId))
                    .thenReturn(Optional.of(testMembership));

            // When
            boolean result = membershipService.isUserOwner(userId, organizationId);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when user is not member")
        void shouldReturnFalseWhenUserIsNotMember() {
            // Given
            Long userId = 999L;
            String organizationId = "org-123";

            when(membershipRepository.findByUserIdAndOrganizationId(userId, organizationId))
                    .thenReturn(Optional.empty());

            // When
            boolean isAdmin = membershipService.isUserAdmin(userId, organizationId);
            boolean isOwner = membershipService.isUserOwner(userId, organizationId);
            OrganizationRole role = membershipService.getUserRole(userId, organizationId);

            // Then
            assertFalse(isAdmin);
            assertFalse(isOwner);
            assertNull(role);
        }

        @Test
        @DisplayName("Should return correct role for user")
        void shouldReturnCorrectRoleForUser() {
            // Given
            Long userId = 1L;
            String organizationId = "org-123";
            testMembership.setRole(OrganizationRole.MEMBER);

            when(membershipRepository.findByUserIdAndOrganizationId(userId, organizationId))
                    .thenReturn(Optional.of(testMembership));

            // When
            OrganizationRole result = membershipService.getUserRole(userId, organizationId);

            // Then
            assertEquals(OrganizationRole.MEMBER, result);
        }
    }

    @Nested
    @DisplayName("Get User ID by Membership Tests")
    class GetUserIdByMembershipTests {

        @Test
        @DisplayName("Should return user ID for valid membership")
        void shouldReturnUserIdForValidMembership() {
            // Given
            Long membershipId = 1L;

            when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(testMembership));

            // When
            Long result = membershipService.getUserIdByMembership(membershipId);

            // Then
            assertEquals(testUser.getId(), result);
            verify(membershipRepository).findById(membershipId);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when membership not found")
        void shouldThrowEntityNotFoundExceptionWhenMembershipNotFound() {
            // Given
            Long membershipId = 999L;

            when(membershipRepository.findById(membershipId)).thenReturn(Optional.empty());

            // When & Then
            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () ->
                    membershipService.getUserIdByMembership(membershipId)
            );

            assertEquals("Membership not found with id: " + membershipId, exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Token Generation Tests")
    class TokenGenerationTests {

        @Test
        @DisplayName("Should generate unique tokens for multiple invitations")
        void shouldGenerateUniqueTokensForMultipleInvitations() {
            // Given
            InvitationCreateDto invitationDto1 = new InvitationCreateDto(
                    "user1@example.com", "org-123", OrganizationRole.MEMBER
            );
            InvitationCreateDto invitationDto2 = new InvitationCreateDto(
                    "user2@example.com", "org-123", OrganizationRole.MEMBER
            );

            Invitation invitation1 = new Invitation();
            invitation1.setId(1L);
            invitation1.setEmail("user1@example.com");
            invitation1.setRole(OrganizationRole.MEMBER);
            invitation1.setInvitedBy(inviterUser);
            invitation1.setOrganization(testOrganization);

            Invitation invitation2 = new Invitation();
            invitation2.setId(2L);
            invitation2.setEmail("user2@example.com");
            invitation2.setRole(OrganizationRole.MEMBER);
            invitation2.setInvitedBy(inviterUser);
            invitation2.setOrganization(testOrganization);

            when(invitationRepository.existsByEmailAndOrganizationId(anyString(), eq("org-123"))).thenReturn(false);
            when(invitationMapper.createDtoToEntity(invitationDto1)).thenReturn(invitation1);
            when(invitationMapper.createDtoToEntity(invitationDto2)).thenReturn(invitation2);
            when(invitationRepository.save(any(Invitation.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(invitationMapper.entityToResponseDto(any(Invitation.class)))
                    .thenReturn(testInvitationResponseDto);
            when(organizationService.getOrganization("org-123")).thenReturn(testOrganizationDto);

            // When
            membershipService.inviteNewUser(invitationDto1, inviterUser);
            membershipService.inviteNewUser(invitationDto2, inviterUser);

            // Then
            verify(invitationRepository, times(2)).save(argThat(invitation -> {
                assertNotNull(invitation.getToken());
                assertEquals(64, invitation.getToken().length()); // 32 bytes = 64 hex chars
                assertNotNull(invitation.getExpiresAt());
                assertTrue(invitation.getExpiresAt().isAfter(LocalDateTime.now().plusDays(6)));
                return true;
            }));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCasesAndErrorHandlingTests {

        @Test
        @DisplayName("Should handle null email in invite member")
        void shouldHandleNullEmailInInviteMember() {
            // Given
            MembershipInviteDto inviteDto = new MembershipInviteDto(
                    null, null, "org-123", OrganizationRole.MEMBER
            );
            Long inviterId = 2L;

            // When & Then
            assertThrows(IllegalArgumentException.class, () ->
                    membershipService.inviteMember(inviteDto, inviterId)
            );

            verify(organizationService).ensureUserIsAdmin(inviterId, "org-123");
        }

        @Test
        @DisplayName("Should handle repository exception during membership save")
        void shouldHandleRepositoryExceptionDuringMembershipSave() {
            // Given
            MembershipInviteDto inviteDto = new MembershipInviteDto(
                    1L, "test@example.com", "org-123", OrganizationRole.MEMBER
            );
            Long inviterId = 2L;

            when(membershipRepository.existsByUserIdAndOrganizationId(1L, "org-123")).thenReturn(false);
            when(membershipMapper.inviteDtoToEntity(inviteDto)).thenReturn(testMembership);
            when(membershipRepository.save(testMembership)).thenThrow(new RuntimeException("Database error"));

            // When & Then
            assertThrows(RuntimeException.class, () ->
                    membershipService.inviteMember(inviteDto, inviterId)
            );

            verify(membershipRepository).save(testMembership);
        }

        @Test
        @DisplayName("Should handle user not found exception in findUserById")
        void shouldHandleUserNotFoundExceptionInFindUserById() {
            // Given
            MembershipInviteDto inviteDto = new MembershipInviteDto(
                    null, "newuser@example.com", "org-123", OrganizationRole.MEMBER
            );
            Long inviterId = 999L; // Non-existent user

            when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
            when(organizationService.getOrganization("org-123")).thenReturn(testOrganizationDto);
            when(userRepository.findById(inviterId)).thenReturn(Optional.empty());

            // When & Then
            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () ->
                    membershipService.inviteMember(inviteDto, inviterId)
            );

            assertEquals("User not found with id: " + inviterId, exception.getMessage());
        }

        @Test
        @DisplayName("Should handle expired invitation in accept invitation")
        void shouldHandleExpiredInvitationInAcceptInvitation() {
            // Given
            String token = "expired-token";
            testInvitation.setExpiresAt(LocalDateTime.now().minusDays(1)); // Expired

            when(invitationRepository.findByTokenAndExpiresAtAfter(eq(token), any(LocalDateTime.class)))
                    .thenReturn(Optional.empty());

            // When & Then
            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () ->
                    membershipService.acceptInvitation(token, testUser)
            );

            assertEquals("Invalid or expired invitation", exception.getMessage());
        }

        @Test
        @DisplayName("Should handle concurrent invitation acceptance")
        void shouldHandleConcurrentInvitationAcceptance() {
            // Given
            String token = "valid-token";

            // First call finds the invitation
            when(invitationRepository.findByTokenAndExpiresAtAfter(eq(token), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(testInvitation));

            // User is already a member (concurrent acceptance scenario)
            when(membershipRepository.existsByUserIdAndOrganizationId(testUser.getId(), testInvitation.getOrganization().getId()))
                    .thenReturn(true);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    membershipService.acceptInvitation(token, testUser)
            );

            assertEquals("User is already a member of this organization", exception.getMessage());
            verify(invitationRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should handle role update for non-existent membership")
        void shouldHandleRoleUpdateForNonExistentMembership() {
            // Given
            Long membershipId = 999L;
            OrganizationRole newRole = OrganizationRole.ADMIN;
            Long updaterId = 2L;

            when(membershipRepository.findById(membershipId)).thenReturn(Optional.empty());

            // When & Then
            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () ->
                    membershipService.updateMemberRole(membershipId, newRole, updaterId)
            );

            assertEquals("Membership not found with id: " + membershipId, exception.getMessage());
        }

        @Test
        @DisplayName("Should validate organization exists when getting members")
        void shouldValidateOrganizationExistsWhenGettingMembers() {
            // Given
            String organizationId = "non-existent-org";
            Long userId = 1L;

            when(organizationService.isUserMember(userId, organizationId))
                    .thenThrow(new EntityNotFoundException("Organization not found"));

            // When & Then
            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () ->
                    membershipService.getOrganizationMembers(organizationId, userId)
            );

            assertEquals("Organization not found", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Integration Scenarios Tests")
    class IntegrationScenariosTests {

        @Test
        @DisplayName("Should handle complete invitation workflow")
        void shouldHandleCompleteInvitationWorkflow() {
            // Given - Create invitation
            InvitationCreateDto invitationDto = new InvitationCreateDto(
                    "newuser@example.com", "org-123", OrganizationRole.MEMBER
            );

            when(invitationRepository.existsByEmailAndOrganizationId("newuser@example.com", "org-123")).thenReturn(false);
            when(invitationMapper.createDtoToEntity(invitationDto)).thenReturn(testInvitation);
            when(invitationRepository.save(any(Invitation.class))).thenReturn(testInvitation);
            when(invitationMapper.entityToResponseDto(testInvitation)).thenReturn(testInvitationResponseDto);
            when(organizationService.getOrganization("org-123")).thenReturn(testOrganizationDto);

            // When - Send invitation
            InvitationResponseDto invitation = membershipService.inviteNewUser(invitationDto, inviterUser);

            // Then - Verify invitation created
            assertNotNull(invitation);
            verify(emailService).sendOrganizationInvitation(eq(testOrganizationDto), eq(inviterUser), anyString(), eq("newuser@example.com"));

            // Given - Accept invitation
            String token = "secure-token-123";
            when(invitationRepository.findByTokenAndExpiresAtAfter(eq(token), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(testInvitation));
            when(membershipRepository.existsByUserIdAndOrganizationId(testUser.getId(), "org-123")).thenReturn(false);
            when(membershipMapper.inviteDtoToEntity(any(MembershipInviteDto.class))).thenReturn(testMembership);
            when(membershipRepository.save(testMembership)).thenReturn(testMembership);
            when(membershipMapper.toDto(testMembership)).thenReturn(testMembershipDto);

            // When - Accept invitation
            OrganizationMembershipDto membership = membershipService.acceptInvitation(token, testUser);

            // Then - Verify membership created and invitation deleted
            assertNotNull(membership);
            verify(invitationRepository).delete(testInvitation);
        }

        @Test
        @DisplayName("Should handle role promotion workflow")
        void shouldHandleRolePromotionWorkflow() {
            // Given - Member exists
            Long membershipId = 1L;
            Long promoterId = 2L;
            testMembership.setRole(OrganizationRole.MEMBER);

            when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(testMembership));
            when(membershipRepository.save(testMembership)).thenReturn(testMembership);
            when(membershipMapper.toDto(testMembership)).thenReturn(testMembershipDto);

            // When - Promote to admin
            OrganizationMembershipDto result = membershipService.updateMemberRole(
                    membershipId, OrganizationRole.ADMIN, promoterId
            );

            // Then - Verify role updated
            assertNotNull(result);
            assertEquals(OrganizationRole.ADMIN, testMembership.getRole());
            verify(organizationService).ensureUserIsAdmin(promoterId, "org-123");

            // Given - Verify admin status
            when(membershipRepository.findByUserIdAndOrganizationId(testUser.getId(), "org-123"))
                    .thenReturn(Optional.of(testMembership));

            // When - Check admin status
            boolean isAdmin = membershipService.isUserAdmin(testUser.getId(), "org-123");

            // Then - Verify admin status
            assertTrue(isAdmin);
        }
    }
}
