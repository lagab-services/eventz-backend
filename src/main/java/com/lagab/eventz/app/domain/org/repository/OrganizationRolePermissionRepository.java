package com.lagab.eventz.app.domain.org.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.lagab.eventz.app.domain.org.model.OrganizationPermission;
import com.lagab.eventz.app.domain.org.model.OrganizationRole;
import com.lagab.eventz.app.domain.org.model.OrganizationRolePermission;

@Repository
public interface OrganizationRolePermissionRepository extends JpaRepository<OrganizationRolePermission, Long> {

    List<OrganizationRolePermission> findByOrganizationIdAndRole(String organizationId, OrganizationRole role);

    List<OrganizationRolePermission> findByOrganizationId(String organizationId);

    Optional<OrganizationRolePermission> findByOrganizationIdAndRoleAndPermission(
            String organizationId, OrganizationRole role, OrganizationPermission permission);

    @Query("SELECT rp FROM OrganizationRolePermission rp WHERE rp.organizationId = :organizationId AND rp.role = :role AND rp.permission = :permission AND rp.granted = true")
    Optional<OrganizationRolePermission> findGrantedPermission(
            String organizationId, OrganizationRole role, OrganizationPermission permission);

    void deleteByOrganizationIdAndRole(String organizationId, OrganizationRole role);

    void deleteByOrganizationId(String organizationId);
}
