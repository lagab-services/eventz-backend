package com.lagab.eventz.app.domain.org.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lagab.eventz.app.domain.org.model.OrganizationMembership;

@Repository
public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, Long> {

    List<OrganizationMembership> findByUserId(Long userId);

    @Query("SELECT m FROM OrganizationMembership m JOIN FETCH m.user WHERE m.organization.id = :organizationId")
    List<OrganizationMembership> findMembersByOrganizationId(@Param("organizationId") String organizationId);

    Optional<OrganizationMembership> findByUserIdAndOrganizationId(Long userId, String organizationId);

    boolean existsByUserIdAndOrganizationId(Long userId, String organizationId);

    boolean existsByUserIdAndOrganizationIdAndRoleIn(Long userId, String organizationId, Collection<String> roles);

    void deleteByOrganizationId(String organizationId);

    @Query("SELECT COUNT(m) FROM OrganizationMembership m WHERE m.organization.id = :organizationId AND m.role = 'admin'")
    long countAdminsByOrganizationId(@Param("organizationId") String organizationId);
}
