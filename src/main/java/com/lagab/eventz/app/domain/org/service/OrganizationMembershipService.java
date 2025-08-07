package com.lagab.eventz.app.domain.org.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lagab.eventz.app.common.exception.BusinessException;
import com.lagab.eventz.app.domain.auth.service.EmailService;
import com.lagab.eventz.app.domain.org.dto.OrganizationDto;
import com.lagab.eventz.app.domain.org.dto.OrganizationMembershipDto;
import com.lagab.eventz.app.domain.org.dto.invitation.InvitationCreateDto;
import com.lagab.eventz.app.domain.org.dto.invitation.InvitationResponseDto;
import com.lagab.eventz.app.domain.org.dto.invitation.MembershipInviteDto;
import com.lagab.eventz.app.domain.org.mapper.InvitationMapper;
import com.lagab.eventz.app.domain.org.mapper.OrganizationMembershipMapper;
import com.lagab.eventz.app.domain.org.model.Invitation;
import com.lagab.eventz.app.domain.org.model.OrganizationMembership;
import com.lagab.eventz.app.domain.org.model.OrganizationRole;
import com.lagab.eventz.app.domain.org.repository.InvitationRepository;
import com.lagab.eventz.app.domain.org.repository.OrganizationMembershipRepository;
import com.lagab.eventz.app.domain.user.model.User;
import com.lagab.eventz.app.domain.user.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OrganizationMembershipService handles all business logic related to organization memberships including:
 * - Member invitations and management
 * - Role assignments and updates
 * - Membership verification and access control
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrganizationMembershipService {

    private final OrganizationService organizationService;
    private final EmailService emailService;
    private final OrganizationMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final InvitationRepository invitationRepository;
    private final OrganizationMembershipMapper membershipMapper;
    private final InvitationMapper invitationMapper;

    /**
     * Invites a user to join an organization
     *
     * @param inviteDto - Membership invitation data
     * @param inviterId - ID of the user initiating the invitation
     * @return MembershipResponseDto - The created membership record or null if invitation sent
     * @throws SecurityException        - If inviter is not admin
     * @throws IllegalArgumentException - If user is already a member
     */
    public OrganizationMembershipDto inviteMember(MembershipInviteDto inviteDto, Long inviterId) {
        // Verify inviter has admin privileges
        organizationService.ensureUserIsAdmin(inviterId, inviteDto.organizationId());

        // Find user by email if userId not provided
        Long userId = inviteDto.userId();
        if (userId == null && inviteDto.email() != null) {
            Optional<User> user = userRepository.findByEmail(inviteDto.email());
            if (user.isEmpty()) {
                // User doesn't exist, send invitation
                OrganizationDto org = organizationService.getOrganization(inviteDto.organizationId());
                User inviter = findUserById(inviterId);

                InvitationCreateDto invitationDto = new InvitationCreateDto(
                        inviteDto.email(),
                        org.id(),
                        inviteDto.role()
                );

                inviteNewUser(invitationDto, inviter);
                return null;
            }
            userId = user.get().getId();
        }
        if (userId == null && inviteDto.email() == null) {
            throw new IllegalArgumentException("User email is required to invite to the organization");
        }

        // Check for existing membership
        boolean existingMembership = membershipRepository.existsByUserIdAndOrganizationId(userId, inviteDto.organizationId());

        if (existingMembership) {
            throw new IllegalArgumentException("User is already a member of this organization");
        }

        // Create membership with complete user info
        MembershipInviteDto completeDto = new MembershipInviteDto(
                userId,
                inviteDto.email(),
                inviteDto.organizationId(),
                inviteDto.role()
        );

        OrganizationMembership membership = membershipMapper.inviteDtoToEntity(completeDto);
        membership = membershipRepository.save(membership);

        // TODO: Implement email notification to new member
        log.info("New member added to organization: userId={}, organizationId={}", userId, inviteDto.organizationId());

        return membershipMapper.toDto(membership);
    }

    /**
     * Retrieves a membership record by ID
     *
     * @param id - Membership record ID
     * @return MembershipResponseDto - The requested membership
     * @throws EntityNotFoundException - If membership is not found
     */
    @Transactional(readOnly = true)
    public OrganizationMembershipDto getMembership(Long id) {
        OrganizationMembership membership = findMembershipById(id);
        return membershipMapper.toDto(membership);
    }

    /**
     * Updates a member's role within an organization
     *
     * @param membershipId - ID of the membership record to update
     * @param role         - Role update data
     * @param updaterId    - ID of the user performing the update
     * @return MembershipResponseDto - The updated membership record
     * @throws SecurityException - If updater lacks admin privileges
     */
    public OrganizationMembershipDto updateMemberRole(Long membershipId, OrganizationRole role, Long updaterId) {
        OrganizationMembership membership = findMembershipById(membershipId);

        // Verify updater has admin privileges
        organizationService.ensureUserIsAdmin(updaterId, membership.getOrganization().getId());

        // Prevent removing last admin
        if (OrganizationRole.ADMIN.equals(membership.getRole()) && !OrganizationRole.ADMIN.equals(role)) {
            long adminCount = membershipRepository.countAdminsByOrganizationId(membership.getOrganization().getId());
            if (adminCount <= 1) {
                throw new IllegalArgumentException("Cannot remove the last admin from organization");
            }
        }

        membership.setRole(role);
        membership = membershipRepository.save(membership);

        return membershipMapper.toDto(membership);
    }

    /**
     * Removes a member from an organization
     *
     * @param membershipId - ID of the membership record to delete
     * @param removerId    - ID of the user performing the removal
     * @throws SecurityException - If remover lacks admin privileges
     */
    public void removeMember(Long membershipId, Long removerId) {
        OrganizationMembership membership = findMembershipById(membershipId);

        // Verify remover has admin privileges
        organizationService.ensureUserIsAdmin(removerId, membership.getOrganization().getId());

        // Prevent removing last admin
        if (OrganizationRole.ADMIN.equals(membership.getRole())) {
            long adminCount = membershipRepository.countAdminsByOrganizationId(membership.getOrganization().getId());
            if (adminCount <= 1) {
                throw new IllegalArgumentException("Cannot remove the last admin from organization");
            }
        }

        // Delete membership record
        membershipRepository.delete(membership);
        log.info("Member removed from organization: membershipId={}", membershipId);
    }

    /**
     * Retrieves all members of an organization
     *
     * @param organizationId - ID of the organization
     * @param userId         - ID of the user requesting the member list
     * @return List<MembershipResponseDto> - List of membership records with user data
     * @throws SecurityException - If requesting user is not a member
     */
    @Transactional(readOnly = true)
    public List<OrganizationMembershipDto> getOrganizationMembers(String organizationId, Long userId) {
        // Verify requesting user is a member
        boolean isMember = organizationService.isUserMember(userId, organizationId);

        if (!isMember) {
            throw new SecurityException("Not authorized to view organization members");
        }

        List<OrganizationMembership> memberships = membershipRepository.findMembersByOrganizationId(organizationId);
        return membershipMapper.entitiesToDtos(memberships);
    }

    /**
     * Invites a new user to join an organization
     *
     * @param invitationDto - Invitation data
     * @param inviter       - User sending the invitation
     * @return InvitationResponseDto - Created invitation record
     */
    public InvitationResponseDto inviteNewUser(InvitationCreateDto invitationDto, User inviter) {
        // Check if invitation already exists
        if (invitationRepository.existsByEmailAndOrganizationId(invitationDto.email(), invitationDto.organizationId())) {
            throw new BusinessException("Invitation already exists for this email and organization");
        }

        // Generate a secure random token
        String token = generateSecureToken();

        // Create invitation entity
        Invitation invitation = invitationMapper.createDtoToEntity(invitationDto);
        invitation.setToken(token);
        invitation.setInvitedBy(inviter);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7)); // Expires in 7 days

        invitation = invitationRepository.save(invitation);

        // Send invitation email
        OrganizationDto organization = organizationService.getOrganization(invitationDto.organizationId());
        emailService.sendOrganizationInvitation(organization, inviter, token, invitationDto.email());

        log.info("Invitation sent: email={}, organizationId={}", invitationDto.email(), invitationDto.organizationId());

        return invitationMapper.entityToResponseDto(invitation);
    }

    /**
     * Accepts an organization invitation
     *
     * @param token - Invitation acceptance token
     * @param user  - User accepting the invitation
     * @return MembershipResponseDto - Created membership
     * @throws EntityNotFoundException - If invitation is not found or expired
     */
    public OrganizationMembershipDto acceptInvitation(String token, User user) {
        // Find valid invitation (not expired)
        Invitation invitation = invitationRepository.findByTokenAndExpiresAtAfter(token, LocalDateTime.now())
                                                    .orElseThrow(() -> new EntityNotFoundException("Invalid or expired invitation"));

        // Create membership data
        MembershipInviteDto membershipDto = new MembershipInviteDto(
                user.getId(),
                user.getEmail(),
                invitation.getOrganization().getId(),
                invitation.getRole()
        );

        // Add user to organization
        OrganizationMembershipDto membership = inviteMember(membershipDto, invitation.getInvitedBy().getId());

        // Delete the used invitation
        invitationRepository.delete(invitation);

        log.info("Invitation accepted: userId={}, organizationId={}", user.getId(), invitation.getOrganization().getId());

        return membership;
    }

    /**
     * Retrieves pending invitations for an organization
     *
     * @param organizationId - ID of the organization
     * @param userId         - ID of the user requesting invitations
     * @return List<InvitationResponseDto> - List of pending invitations
     */
    @Transactional(readOnly = true)
    public List<InvitationResponseDto> getPendingInvitations(String organizationId, Long userId) {
        // Verify user has admin privileges
        organizationService.ensureUserIsAdmin(userId, organizationId);

        List<Invitation> invitations = invitationRepository.findByOrganizationId(organizationId);
        return invitationMapper.entitiesToResponseDtos(invitations);
    }

    /**
     * Cancels a pending invitation
     *
     * @param invitationId - ID of the invitation to cancel
     * @param userId       - ID of the user canceling the invitation
     */
    public void cancelInvitation(Long invitationId, Long userId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                                                    .orElseThrow(() -> new EntityNotFoundException("Invitation not found"));

        // Verify user has admin privileges
        organizationService.ensureUserIsAdmin(userId, invitation.getOrganization().getId());

        invitationRepository.delete(invitation);
        log.info("Invitation cancelled: invitationId={}", invitationId);
    }

    /**
     * Helper method to find membership by ID
     *
     * @param id - Membership ID
     * @return OrganizationMembership - The found membership
     * @throws EntityNotFoundException - If membership is not found
     */
    private OrganizationMembership findMembershipById(Long id) {
        return membershipRepository.findById(id)
                                   .orElseThrow(() -> new EntityNotFoundException("Membership not found with id: " + id));
    }

    public Long getUserIdByMembership(Long membershipId) {
        OrganizationMembership membership = findMembershipById(membershipId);
        return membership.getUser().getId();
    }

    /**
     * Helper method to find user by ID
     *
     * @param id - User ID
     * @return User - The found user
     * @throws EntityNotFoundException - If user is not found
     */
    private User findUserById(Long id) {
        return userRepository.findById(id)
                             .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }

    /**
     * Generates a secure random token for invitations
     *
     * @return String - Secure random token
     */
    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        StringBuilder token = new StringBuilder();
        for (byte b : bytes) {
            token.append(String.format("%02x", b));
        }
        return token.toString();
    }

    /**
     * Checks if a user has admin privileges (ADMIN or OWNER role) in an organization
     *
     * @param userId         - ID of the user
     * @param organizationId - ID of the organization
     * @return true if the user has admin privileges
     */
    @Transactional(readOnly = true)
    public boolean isUserAdmin(Long userId, String organizationId) {
        Optional<OrganizationMembership> membership = membershipRepository
                .findByUserIdAndOrganizationId(userId, organizationId);

        if (membership.isEmpty()) {
            return false;
        }

        OrganizationRole role = membership.get().getRole();
        return role == OrganizationRole.ADMIN || role == OrganizationRole.OWNER;
    }

    /**
     * Checks if a user is the owner of an organization
     *
     * @param userId         - ID of the user
     * @param organizationId - ID of the organization
     * @return true if the user is the owner
     */
    @Transactional(readOnly = true)
    public boolean isUserOwner(Long userId, String organizationId) {
        Optional<OrganizationMembership> membership = membershipRepository
                .findByUserIdAndOrganizationId(userId, organizationId);

        return membership.isPresent() && membership.get().getRole() == OrganizationRole.OWNER;
    }

    /**
     * Gets the role of a user in an organization
     *
     * @param userId         - ID of the user
     * @param organizationId - ID of the organization
     * @return OrganizationRole - The user's role, or null if not a member
     */
    @Transactional(readOnly = true)
    public OrganizationRole getUserRole(Long userId, String organizationId) {
        Optional<OrganizationMembership> membership = membershipRepository
                .findByUserIdAndOrganizationId(userId, organizationId);

        return membership.map(OrganizationMembership::getRole).orElse(null);
    }

}
