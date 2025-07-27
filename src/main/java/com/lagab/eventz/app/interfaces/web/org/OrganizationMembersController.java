package com.lagab.eventz.app.interfaces.web.org;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lagab.eventz.app.domain.org.dto.InvitationResponseDto;
import com.lagab.eventz.app.domain.org.dto.MembershipInviteDto;
import com.lagab.eventz.app.domain.org.dto.OrganizationMembershipDto;
import com.lagab.eventz.app.domain.org.service.OrganizationMembershipService;
import com.lagab.eventz.app.domain.user.model.User;
import com.lagab.eventz.app.interfaces.web.org.dto.UpdateMemberRoleRequestDto;
import com.lagab.eventz.app.util.SecurityUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller for managing organization memberships
 * Handles invitations, role updates, and member removal
 */
@RestController
@RequestMapping("/api/organizations/{organizationId}/members")
@RequiredArgsConstructor
public class OrganizationMembersController {

    private final OrganizationMembershipService organizationMembershipService;

    /**
     * Invite a new member to an organization
     * POST /api/organizations/{organizationId}/members
     *
     * @param organizationId Organization ID from path
     * @param request        Invitation details
     * @return Created membership response
     */
    @PostMapping
    @PreAuthorize("@organizationSecurityService.canInviteMembers(authentication.principal.id, #organizationId)")
    public ResponseEntity<OrganizationMembershipDto> invite(
            @PathVariable String organizationId,
            @Valid @RequestBody MembershipInviteDto request) {

        Long userId = SecurityUtils.getCurrentUserId();

        MembershipInviteDto requestWithOrgId = new MembershipInviteDto(null, request.email(), organizationId, request.role());

        OrganizationMembershipDto membership = organizationMembershipService.inviteMember(requestWithOrgId, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(membership);

    }

    /**
     * List all members of an organization
     * GET /api/organizations/{organizationId}/members
     *
     * @param organizationId Organization ID from path
     * @return List of organization members
     */
    @GetMapping
    public ResponseEntity<List<OrganizationMembershipDto>> listAllMembers(@PathVariable String organizationId) {

        Long userId = SecurityUtils.getCurrentUserId();

        List<OrganizationMembershipDto> members = organizationMembershipService.getOrganizationMembers(organizationId, userId);

        return ResponseEntity.ok(members);

    }

    /**
     * Update a member's role in the organization
     * PATCH /api/organizations/{organizationId}/members/{memberId}
     *
     * @param organizationId Organization ID from path
     * @param memberId       Member ID from path
     * @param request        New role information
     * @return Updated membership response
     */
    @PatchMapping("/{memberId}")
    @PreAuthorize("@organizationSecurityService.canManageMembers(authentication.principal.id, #organizationId)")
    public ResponseEntity<OrganizationMembershipDto> updateRole(
            @PathVariable String organizationId,
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateMemberRoleRequestDto request) {

        Long userId = SecurityUtils.getCurrentUserId();

        OrganizationMembershipDto membership = organizationMembershipService
                .updateMemberRole(memberId, request.role(), userId);

        return ResponseEntity.ok(membership);

    }

    /**
     * Remove a member from the organization
     * DELETE /api/organizations/{organizationId}/members/{memberId}
     *
     * @param memberId Member ID from path
     * @return Empty response
     */
    @DeleteMapping("/{memberId}")
    @PreAuthorize("@organizationSecurityService.canRemoveMember(authentication.principal.id, #organizationId, @organizationMembershipService.getUserIdByMembership(#memberId))")
    public ResponseEntity<Void> remove(@PathVariable String organizationId, @PathVariable Long memberId) {

        Long userId = SecurityUtils.getCurrentUserId();

        organizationMembershipService.removeMember(memberId, userId);

        return ResponseEntity.noContent().build();

    }

    /**
     * Accept an organization invitation
     * POST /api/organizations/invitations/accept
     *
     * @param token Token
     * @return Accepted membership response
     */
    @PostMapping("/invitations/accept")
    public ResponseEntity<OrganizationMembershipDto> acceptInvitation(@Valid @RequestParam String token) {

        User currentUser = SecurityUtils.getCurrentUser();

        OrganizationMembershipDto membership = organizationMembershipService.acceptInvitation(token, currentUser);

        return ResponseEntity.ok(membership);

    }

    /**
     * List pending invitations for an organization
     * GET /api/organizations/{organizationId}/members/invitations
     *
     * @param organizationId Organization ID from path
     * @return List of pending invitations
     */
    @GetMapping("/invitations")
    @PreAuthorize("@organizationSecurityService.canInviteMembers(authentication.principal.id, #organizationId)")
    public ResponseEntity<List<InvitationResponseDto>> listPendingInvitations(@PathVariable String organizationId) {

        Long userId = SecurityUtils.getCurrentUserId();

        List<InvitationResponseDto> pendingInvitations = organizationMembershipService.getPendingInvitations(organizationId, userId);

        return ResponseEntity.ok(pendingInvitations);
    }

}
