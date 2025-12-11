package com.lagab.eventz.app.domain.event.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.lagab.eventz.app.domain.event.model.TicketCategory;

@Repository
public interface TicketCategoryRepository extends JpaRepository<TicketCategory, Long> {

    List<TicketCategory> findByEventIdOrderByDisplayOrderAsc(Long eventId);

    @Modifying
    @Query("UPDATE TicketCategory tc SET tc.displayOrder = :order WHERE tc.id = :id")
    void updateDisplayOrder(Long id, Integer order);

    //

    List<TicketCategory> findByEventIdOrderByDisplayOrderAscIdAsc(Long eventId);

    List<TicketCategory> findByEventIdAndIsActiveTrue(Long eventId);

    @Query("SELECT tc FROM TicketCategory tc WHERE tc.event.id = :eventId AND tc.isActive = true ORDER BY tc.displayOrder ASC, tc.id ASC")
    List<TicketCategory> findActiveByEventIdOrderByDisplayOrder(@Param("eventId") Long eventId);

    boolean existsByEventIdAndName(Long eventId, String name);

    boolean existsByEventIdAndNameAndIdNot(Long eventId, String name, Long id);

    @Modifying
    @Transactional
    @Query("UPDATE TicketCategory tc SET tc.displayOrder = tc.displayOrder + 1 WHERE tc.event.id = :eventId AND tc.displayOrder >= :order")
    int incrementDisplayOrderFrom(@Param("eventId") Long eventId, @Param("order") Integer order);

    @Query("SELECT MAX(tc.displayOrder) FROM TicketCategory tc WHERE tc.event.id = :eventId")
    Optional<Integer> findMaxDisplayOrderByEventId(@Param("eventId") Long eventId);

    @Query("SELECT tc FROM TicketCategory tc LEFT JOIN FETCH tc.ticketTypes WHERE tc.id = :id")
    Optional<TicketCategory> findByIdWithTicketTypes(@Param("id") Long id);

    @Query("SELECT tc FROM TicketCategory tc LEFT JOIN FETCH tc.ticketTypes tt WHERE tc.event.id = :eventId ORDER BY tc.displayOrder ASC, tc.id ASC")
    List<TicketCategory> findByEventIdWithTicketTypes(@Param("eventId") Long eventId);
}
