package com.lagab.eventz.app.domain.ticket.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lagab.eventz.app.domain.ticket.entity.Ticket;
import com.lagab.eventz.app.domain.ticket.entity.TicketStatus;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {
    Optional<Ticket> findByTicketCode(String ticketCode);

    Page<Ticket> findByOrderIdOrderByCreatedAt(Long orderId, Pageable pageable);

    Optional<Ticket> findByQrCode(String qrCode);

    Page<Ticket> findByEventIdAndStatus(Long eventId, TicketStatus status, Pageable pageable);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.event.id = :eventId AND t.status = :status")
    long countByEventIdAndStatus(@Param("eventId") Long eventId, @Param("status") TicketStatus status);

    @Query("SELECT t FROM Ticket t WHERE t.status IN :statuses ORDER BY t.createdAt DESC")
    Page<Ticket> findByUserIdAndStatusIn(@Param("statuses") List<TicketStatus> statuses, Pageable pageable);
}
