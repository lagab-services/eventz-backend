package com.lagab.eventz.app.domain.order.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lagab.eventz.app.domain.order.model.Order;
import com.lagab.eventz.app.domain.order.model.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    @Query("SELECT o FROM Order o WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.event.id = :eventId ORDER BY o.createdAt DESC")
    Page<Order> findByEventIdOrderByCreatedAtDesc(Long eventId, Pageable pageable);

    //new

    /**
     * Retrieves expired unprocessed orders
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING' AND o.paymentDeadline < :now")
    List<Order> findExpiredPendingOrders(@Param("now") LocalDateTime now);

    /**
     * Retrieves orders for an event
     */
    @Query("SELECT DISTINCT o FROM Order o JOIN o.orderItems oi WHERE oi.ticketType.event.id = :eventId")
    Page<Order> findByEventId(@Param("eventId") Long eventId, Pageable pageable);

    /**
     * Counts paid orders for an event
     */
    @Query("SELECT COUNT(DISTINCT o) FROM Order o JOIN o.orderItems oi " +
            "WHERE oi.ticketType.event.id = :eventId AND o.status = 'PAID'")
    Long countPaidOrdersByEventId(@Param("eventId") Long eventId);

    /**
     * Calculates revenue for an event
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o JOIN o.orderItems oi " +
            "WHERE oi.ticketType.event.id = :eventId AND o.status = 'PAID'")
    BigDecimal sumRevenueByEventId(@Param("eventId") Long eventId);

    /**
     * Retrieves orders created within a period
     */
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate ORDER BY o.createdAt DESC")
    Page<Order> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate, Pageable pageable);

    @Query("SELECT o FROM Order o JOIN o.orderItems i WHERE i.ticketType.event.id = :eventId AND o.status = :status")
    Page<Order> findByEventIdAndStatus(@Param("eventId") Long eventId, @Param("status") OrderStatus status, Pageable pageable);

    Optional<Order> findByOrderNumberAndBillingEmailIgnoreCase(String orderNumber, String billingEmail);
}
