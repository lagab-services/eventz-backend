package com.lagab.eventz.app.domain.org.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lagab.eventz.app.domain.org.model.Organization;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, String> {

    Optional<Organization> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT o FROM OrganizationMembership m JOIN m.organization o WHERE m.user.id = :userId")
    List<Organization> findByUserId(@Param("userId") Long userId);
}
