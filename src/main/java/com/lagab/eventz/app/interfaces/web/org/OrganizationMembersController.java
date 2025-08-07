package com.lagab.eventz.app.interfaces.web.org;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lagab.eventz.app.domain.org.dto.OrganizationMembershipDto;
import com.lagab.eventz.app.domain.org.dto.invitation.InvitationResponseDto;
import com.lagab.eventz.app.domain.org.dto.invitation.MembershipInviteDto;
import com.lagab.eventz.app.domain.org.service.OrganizationMembershipService;
import com.lagab.eventz.app.domain.user.model.User;
import com.lagab.eventz.app.interfaces.web.org.annotation.RequireOrganizationPermission;
import com.lagab.eventz.app.interfaces.web.org.dto.UpdateMemberRoleRequestDto;
import com.lagab.eventz.app.util.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller for managing organization memberships
 * Handles invitations, role updates, and member removal
 */
@RestController
@RequestMapping("/api/organizations/{orgId}/members")
@RequiredArgsConstructor
@Tag(name = "Organization Members", description = "API for managing organization memberships including invitations, role updates, and member removal")
@SecurityRequirement(name = "bearerAuth")
public class OrganizationMembersController {

    private final OrganizationMembershipService organizationMembershipService;

    /**
     * Invite a new member to an organization
     * POST /api/organizations/{orgId}/members
     *
     * @param orgId   Organization ID from path
     * @param request Invitation details
     * @return Created membership response
     */
    @Operation(
            summary = "Invite a new member",
            description = "Sends an invitation to a user to join the organization with a specified role. Requires MEMBER_INVITE permission. An email invitation will be sent to the specified email address."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Invitation successfully sent and membership created",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OrganizationMembershipDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or validation errors",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\": \"Invalid email format\", \"errors\": [\"Email must be valid\"]}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - insufficient permissions to invite members",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\": \"You do not have permission to invite members\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Organization not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "User is already a member of the organization",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\": \"User is already a member of this organization\"}")
                    )
            )
    })
    @PostMapping
    @RequireOrganizationPermission(permission = "MEMBER_INVITE")
    public ResponseEntity<OrganizationMembershipDto> invite(
            @Parameter(description = "Organization identifier", required = true, example = "org-123")
            @PathVariable String orgId,
            @Parameter(description = "Invitation details including email and role", required = true)
            @Valid @RequestBody MembershipInviteDto request) {

        Long userId = SecurityUtils.getCurrentUserId();

        MembershipInviteDto requestWithOrgId = new MembershipInviteDto(null, request.email(), orgId, request.role());

        OrganizationMembershipDto membership = organizationMembershipService.inviteMember(requestWithOrgId, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(membership);

    }

    /**
     * List all members of an organization
     * GET /api/organizations/{orgId}/members
     *
     * @param orgId Organization ID from path
     * @return List of organization members
     */
    @Operation(
            summary = "List organization members",
            description = "Retrieves all members of the organization including their roles and membership details. Requires MEMBER_VIEW permission."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved organization members",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OrganizationMembershipDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - insufficient permissions to view members",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\": \"You do not have permission to view members\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Organization not found",
                    content = @Content
            )
    })
    @GetMapping
    @RequireOrganizationPermission(permission = "MEMBER_VIEW")
    public ResponseEntity<List<OrganizationMembershipDto>> listAllMembers(
            @Parameter(description = "Organization identifier", required = true, example = "org-123")
            @PathVariable String orgId) {

        Long userId = SecurityUtils.getCurrentUserId();

        List<OrganizationMembershipDto> members = organizationMembershipService.getOrganizationMembers(orgId, userId);

        return ResponseEntity.ok(members);

    }

    /**
     * Update a member's role in the organization
     * PATCH /api/organizations/{orgId}/members/{memberId}
     *
     * @param orgId    Organization ID from path
     * @param memberId Member ID from path
     * @param request  New role information
     * @return Updated membership response
     */
    @Operation(
            summary = "Update member role",
            description = "Updates the role of an existing organization member. Requires MEMBER_VIEW permission. Users cannot modify their own role or promote members to a role higher than their own."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Member role successfully updated",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OrganizationMembershipDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or validation errors",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\": \"Invalid role specified\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - insufficient permissions to update member roles",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\": \"You cannot assign a role higher than your own\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Organization or member not found",
                    content = @Content
            )
    })
    @PatchMapping("/{memberId}")
    @RequireOrganizationPermission(permission = "MEMBER_VIEW")
    public ResponseEntity<OrganizationMembershipDto> updateRole(
            @Parameter(description = "Organization identifier", required = true, example = "org-123")
            @PathVariable String orgId,
            @Parameter(description = "Member identifier", required = true, example = "123")
            @PathVariable Long memberId,
            @Parameter(description = "New role information", required = true)
            @Valid @RequestBody UpdateMemberRoleRequestDto request) {

        Long userId = SecurityUtils.getCurrentUserId();

        OrganizationMembershipDto membership = organizationMembershipService
                .updateMemberRole(memberId, request.role(), userId);

        return ResponseEntity.ok(membership);

    }

    /**
     * Remove a member from the organization
     * DELETE /api/organizations/{orgId}/members/{memberId}
     *
     * @param memberId Member ID from path
     * @return Empty response
     */
    @Operation(
            summary = "Remove organization member",
            description = "Removes a member from the organization. Requires MEMBER_REMOVE permission. Organization owners cannot be removed, and users cannot remove themselves."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Member successfully removed from organization"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - insufficient permissions to remove members or attempting to remove owner/self",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "Insufficient permissions",
                                            value = "{\"message\": \"You do not have permission to remove members\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Cannot remove owner",
                                            value = "{\"message\": \"Cannot remove organization owner\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Cannot remove self",
                                            value = "{\"message\": \"You cannot remove yourself from the organization\"}"
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Organization or member not found",
                    content = @Content
            )
    })
    @DeleteMapping("/{memberId}")
    @RequireOrganizationPermission(permission = "MEMBER_REMOVE")
    public ResponseEntity<Void> remove(
            @Parameter(description = "Organization identifier", required = true, example = "org-123")
            @PathVariable String orgId,
            @Parameter(description = "Member identifier", required = true, example = "123")
            @PathVariable Long memberId) {

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
    @Operation(
            summary = "Accept organization invitation",
            description = "Accepts an organization invitation using the provided token. The token is typically received via email. Upon acceptance, the user becomes a member of the organization with the role specified in the invitation."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Invitation successfully accepted and membership created",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OrganizationMembershipDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid or missing token",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\": \"Invalid invitation token\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Invitation not found or expired",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\": \"Invitation not found or has expired\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "User is already a member of the organization",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\": \"You are already a member of this organization\"}")
                    )
            )
    })
    @PostMapping("/invitations/accept")
    public ResponseEntity<OrganizationMembershipDto> acceptInvitation(
            @Parameter(description = "Invitation token received via email", required = true, example = "abc123def456")
            @Valid @RequestParam String token) {

        User currentUser = SecurityUtils.getCurrentUser();

        OrganizationMembershipDto membership = organizationMembershipService.acceptInvitation(token, currentUser);

        return ResponseEntity.ok(membership);

    }

    /**
     * List pending invitations for an organization
     * GET /api/organizations/{orgId}/members/invitations
     *
     * @param orgId Organization ID from path
     * @return List of pending invitations
     */
    @Operation(
            summary = "List pending invitations",
            description = "Retrieves all pending (not yet accepted) invitations for the organization. Requires MEMBER_INVITE permission. Shows invitations that have been sent but not yet accepted by the recipients."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved pending invitations",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = InvitationResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - insufficient permissions to view invitations",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\": \"You do not have permission to view invitations\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Organization not found",
                    content = @Content
            )
    })
    @GetMapping("/invitations")
    @RequireOrganizationPermission(permission = "MEMBER_INVITE")
    public ResponseEntity<List<InvitationResponseDto>> listPendingInvitations(
            @Parameter(description = "Organization identifier", required = true, example = "org-123")
            @PathVariable String orgId) {

        Long userId = SecurityUtils.getCurrentUserId();

        List<InvitationResponseDto> pendingInvitations = organizationMembershipService.getPendingInvitations(orgId, userId);

        return ResponseEntity.ok(pendingInvitations);
    }

}
