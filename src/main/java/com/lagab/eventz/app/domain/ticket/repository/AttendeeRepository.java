package com.lagab.eventz.app.domain.ticket.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lagab.eventz.app.domain.ticket.entity.Attendee;
import com.lagab.eventz.app.domain.ticket.entity.CheckInStatus;

public interface AttendeeRepository extends JpaRepository<Attendee, Long> {

    List<Attendee> findByEventId(Long eventId);

    Page<Attendee> findByEventId(Long eventId, Pageable pageable);

    List<Attendee> findByOrderId(Long orderId);

    List<Attendee> findByOrderIdAndTicketIsNull(Long orderId);

    @Query("SELECT a FROM Attendee a WHERE a.email = :email AND a.event.id = :eventId")
    Optional<Attendee> findByEmailAndEventId(@Param("email") String email, @Param("eventId") Long eventId);

    @Query("SELECT a FROM Attendee a WHERE a.ticket.ticketCode = :ticketNumber")
    Optional<Attendee> findByTicketNumber(@Param("ticketNumber") String ticketNumber);

    @Query("SELECT a FROM Attendee a WHERE a.event.id = :eventId AND a.checkInStatus = :status")
    Page<Attendee> findByEventIdAndCheckInStatus(@Param("eventId") Long eventId, @Param("status") CheckInStatus status, Pageable pageable);

    @Query("""
            SELECT a FROM Attendee a 
            WHERE (:eventId IS NULL OR a.event.id = :eventId)
            AND (:name IS NULL OR LOWER(CONCAT(a.firstName, ' ', a.lastName)) LIKE LOWER(CONCAT('%', :name, '%')))
            AND (:email IS NULL OR LOWER(a.email) LIKE LOWER(CONCAT('%', :email, '%')))
            AND (:status IS NULL OR a.checkInStatus = :status)
            AND (:ticketTypeId IS NULL OR a.ticket.ticketType.id = :ticketTypeId)
            """)
    List<Attendee> findByCriteria(
            @Param("eventId") Long eventId,
            @Param("name") String name,
            @Param("email") String email,
            @Param("status") CheckInStatus status,
            @Param("ticketTypeId") Long ticketTypeId
    );

    @Query("SELECT COUNT(a) FROM Attendee a WHERE a.event.id = :eventId AND a.checkInStatus = 'CHECKED_IN'")
    long countCheckedInByEventId(@Param("eventId") Long eventId);
}
