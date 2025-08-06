package com.lagab.eventz.app.domain.event.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lagab.eventz.app.domain.event.projection.TicketTypeStatsProjection;
import com.lagab.eventz.app.domain.ticket.entity.TicketType;

@Repository
public interface TicketTypeRepository extends JpaRepository<TicketType, Long> {

    // Search by event
    @Query("""
            SELECT tt FROM TicketType tt 
            JOIN FETCH tt.event e 
            WHERE e.id = :eventId 
            ORDER BY tt.sortOrder ASC, tt.id ASC
            """)
    List<TicketType> findByEventIdOrderBySortOrder(@Param("eventId") Long eventId);

    // Active ticket types for an event
    @Query("""
            SELECT tt FROM TicketType tt 
            WHERE tt.event.id = :eventId 
            AND tt.isActive = true 
            ORDER BY tt.sortOrder ASC, tt.id ASC
            """)
    List<TicketType> findActiveByEventId(@Param("eventId") Long eventId);

    // Ticket types currently on sale for an event
    @Query("""
            SELECT tt FROM TicketType tt 
            WHERE tt.event.id = :eventId 
            AND tt.isActive = true 
            AND (tt.saleStart IS NULL OR tt.saleStart <= :now) 
            AND (tt.saleEnd IS NULL OR tt.saleEnd >= :now)
            AND (tt.quantityAvailable IS NULL OR tt.quantityAvailable > tt.quantitySold)
            ORDER BY tt.sortOrder ASC, tt.id ASC
            """)
    List<TicketType> findOnSaleByEventId(@Param("eventId") Long eventId, @Param("now") LocalDateTime now);

    // Statistics for an event
    @Query("""
            SELECT 
                COUNT(tt) as totalTicketTypes,
                SUM(CASE WHEN tt.isActive = true THEN 1 ELSE 0 END) as activeTicketTypes,
                SUM(CASE WHEN tt.quantityAvailable <= tt.quantitySold THEN 1 ELSE 0 END) as soldOutTicketTypes,
                SUM(COALESCE(tt.capacity, 0)) as totalCapacity,
                SUM(COALESCE(tt.quantitySold, 0)) as totalSold,
                SUM(COALESCE(tt.quantityAvailable, 0) - COALESCE(tt.quantitySold, 0)) as totalRemaining,
                SUM(COALESCE(tt.quantitySold, 0) * tt.price) as totalRevenue,
                AVG(tt.price) as averagePrice
            FROM TicketType tt 
            WHERE tt.event.id = :eventId
            """)
    Optional<TicketTypeStatsProjection> getStatsByEventId(@Param("eventId") Long eventId);

    // Update sold quantity
    @Modifying
    @Query("UPDATE TicketType tt SET tt.quantitySold = tt.quantitySold + :quantity WHERE tt.id = :ticketTypeId")
    int updateQuantitySold(@Param("ticketTypeId") Long ticketTypeId, @Param("quantity") Integer quantity);

    // Expired ticket types (sale ended)
    @Query("SELECT tt FROM TicketType tt WHERE tt.saleEnd < :currentDate AND tt.isActive = true")
    Page<TicketType> findExpiredTicketTypes(@Param("currentDate") LocalDateTime currentDate, Pageable pageable);

    // Ticket types with upcoming sales
    @Query("SELECT tt FROM TicketType tt WHERE tt.saleStart BETWEEN :now AND :futureDate AND tt.isActive = true")
    Page<TicketType> findUpcomingSaleTicketTypes(@Param("now") LocalDateTime now, @Param("futureDate") LocalDateTime futureDate, Pageable pageable);

    // Check availability
    @Query("SELECT tt.quantityAvailable - tt.quantitySold FROM TicketType tt WHERE tt.id = :ticketTypeId")
    Optional<Integer> getAvailableQuantity(@Param("ticketTypeId") Long ticketTypeId);

    // Ticket types sorted by order
    List<TicketType> findByEventIdOrderBySortOrderAscIdAsc(Long eventId);
}
