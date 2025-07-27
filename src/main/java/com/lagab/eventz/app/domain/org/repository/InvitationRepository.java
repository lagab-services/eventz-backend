package com.lagab.eventz.app.domain.org.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lagab.eventz.app.domain.org.model.Invitation;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    Optional<Invitation> findByTokenAndExpiresAtAfter(String token, LocalDateTime dateTime);

    List<Invitation> findByOrganizationId(String organizationId);

    boolean existsByEmailAndOrganizationId(String email, String organizationId);

    @Query("SELECT i FROM Invitation i WHERE i.expiresAt < :dateTime")
    List<Invitation> findExpiredInvitations(@Param("dateTime") LocalDateTime dateTime);
}
