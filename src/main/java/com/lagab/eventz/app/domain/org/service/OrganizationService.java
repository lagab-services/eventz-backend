package com.lagab.eventz.app.domain.org.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lagab.eventz.app.domain.org.dto.OrganizationDto;
import com.lagab.eventz.app.domain.org.mapper.OrganizationMapper;
import com.lagab.eventz.app.domain.org.model.Organization;
import com.lagab.eventz.app.domain.org.model.OrganizationMembership;
import com.lagab.eventz.app.domain.org.model.OrganizationRole;
import com.lagab.eventz.app.domain.org.repository.OrganizationMembershipRepository;
import com.lagab.eventz.app.domain.org.repository.OrganizationRepository;
import com.lagab.eventz.app.domain.user.model.User;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * OrganizationService handles all business logic related to organizations including:
 * - Organization creation and management
 * - Membership verification
 * - Role-based access control
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final OrganizationMapper organizationMapper;
    private final OrganizationPermissionService permissionService;

    /**
     * Creates a new organization and assigns the creator as admin
     *
     * @param createDto - Organization creation data
     * @param user      - the user creating the organization
     * @return OrganizationResponseDto - The newly created organization
     */
    public OrganizationDto createOrganization(OrganizationDto createDto, User user) {
        // Convert DTO to entity
        Organization organization = organizationMapper.toEntity(createDto);

        // Save organization
        organization = organizationRepository.save(organization);

        // Automatically add creator as admin member
        OrganizationMembership membership = new OrganizationMembership();
        membership.setUser(user);
        membership.setOrganization(organization);
        membership.setRole(OrganizationRole.OWNER);

        membershipRepository.save(membership);

        // initialize default permissions
        permissionService.initializeDefaultPermissions(organization.getId());

        // Convert entity to response DTO
        return organizationMapper.toDto(organization);
    }

    /**
     * Retrieves all organizations for a specific user
     *
     * @param userId - ID of the user
     * @return List<OrganizationResponseDto> - List of organizations the user belongs to
     */
    @Transactional(readOnly = true)
    public List<OrganizationDto> getUserOrganizations(Long userId) {
        List<Organization> organizations = organizationRepository.findByUserId(userId);
        return organizationMapper.toDtos(organizations);
    }

    /**
     * Retrieves an organization by its ID
     *
     * @param id - Organization ID
     * @return OrganizationResponseDto - The requested organization
     * @throws EntityNotFoundException - If organization is not found
     */
    @Transactional(readOnly = true)
    public OrganizationDto getOrganization(String id) {
        Organization organization = findOrganizationById(id);
        return organizationMapper.toDto(organization);
    }

    /**
     * Retrieves an organization by its ID
     *
     * @param id - Organization ID
     * @return OrganizationResponseDto - The requested organization
     * @throws EntityNotFoundException - If organization is not found
     */
    @Transactional(readOnly = true)
    public Organization getOrganizationById(String id) {
        return findOrganizationById(id);
    }

    /**
     * Updates an organization's details
     *
     * @param id        - Organization ID to update
     * @param updateDto - Partial organization data to update
     * @param userId    - ID of the user performing the update
     * @return OrganizationResponseDto - The updated organization
     * @throws SecurityException - If user is not admin
     */
    public OrganizationDto updateOrganization(String id, OrganizationDto updateDto, Long userId) {
        // Verify organization exists
        Organization organization = findOrganizationById(id);

        // Verify user has admin privileges
        ensureUserIsAdmin(userId, id);

        // Apply updates using mapper
        organizationMapper.updateEntityFromDto(updateDto, organization);

        // Save updated organization
        organization = organizationRepository.save(organization);

        // Convert to response DTO
        return organizationMapper.toDto(organization);
    }

    /**
     * Deletes an organization
     *
     * @param id     - Organization ID to delete
     * @param userId - ID of the user performing the deletion
     * @throws SecurityException - If user is not admin
     */
    public void deleteOrganization(String id, Long userId) {
        // Verify organization exists
        Organization organization = findOrganizationById(id);

        // Verify user has admin privileges
        ensureUserIsAdmin(userId, id);

        // Delete permissions first
        permissionService.deleteOrganizationPermissions(id);

        // Delete related memberships
        membershipRepository.deleteByOrganizationId(id);

        // Delete organization
        organizationRepository.delete(organization);
    }

    /**
     * Verifies if a user has admin privileges for an organization
     *
     * @param userId         - ID of the user to check
     * @param organizationId - ID of the organization
     * @throws SecurityException - If user is not admin
     */
    public void ensureUserIsAdmin(Long userId, String organizationId) {
        boolean isAdmin = membershipRepository.existsByUserIdAndOrganizationIdAndRoleIn(userId, organizationId,
                List.of(OrganizationRole.ADMIN.name(), OrganizationRole.OWNER.name()));

        if (!isAdmin) {
            throw new SecurityException("Not authorized to perform this action");
        }
    }

    /**
     * Checks if a user is a member of an organization
     *
     * @param userId         - ID of the user to check
     * @param organizationId - ID of the organization
     * @return boolean - True if user is a member, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isUserMember(Long userId, String organizationId) {
        return membershipRepository.existsByUserIdAndOrganizationId(userId, organizationId);
    }

    /**
     * Helper method to find organization by ID
     *
     * @param id - Organization ID
     * @return Organization - The found organization
     * @throws EntityNotFoundException - If organization is not found
     */
    private Organization findOrganizationById(String id) {
        return organizationRepository.findById(id)
                                     .orElseThrow(() -> new EntityNotFoundException("Organization not found with id: " + id));
    }

    public boolean isSlugTaken(String slug) {
        return organizationRepository.existsBySlug(slug);
    }
}
