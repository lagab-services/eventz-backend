package com.lagab.eventz.app.domain.order.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lagab.eventz.app.domain.order.dto.OrderResponse;
import com.lagab.eventz.app.domain.order.dto.TrackOrderRequest;
import com.lagab.eventz.app.domain.order.mapper.OrderMapper;
import com.lagab.eventz.app.domain.order.model.Order;
import com.lagab.eventz.app.domain.order.model.OrderStatus;
import com.lagab.eventz.app.domain.order.repository.OrderRepository;
import com.lagab.eventz.app.domain.ticket.entity.Attendee;
import com.lagab.eventz.app.domain.ticket.entity.Ticket;
import com.lagab.eventz.app.domain.ticket.repository.AttendeeRepository;
import com.lagab.eventz.app.domain.ticket.repository.TicketRepository;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuestOrderServiceTest {

    @Mock
    OrderRepository orderRepository;
    @Mock
    OrderMapper orderMapper;
    @Mock
    AttendeeRepository attendeeRepository;
    @Mock
    TicketRepository ticketRepository;

    @InjectMocks
    GuestOrderService service;

    @Test
    void trackOrder_returnsResponse() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNumber("ORD-321");
        order.setStatus(OrderStatus.PAID);
        order.setTotalAmount(BigDecimal.TEN);
        order.setCreatedAt(LocalDateTime.now());

        when(orderRepository.findByOrderNumberAndBillingEmailIgnoreCase("ORD-321", "guest@example.com"))
                .thenReturn(Optional.of(order));

        OrderResponse mapped = new OrderResponse(1L, "ORD-321", OrderStatus.PAID, BigDecimal.TEN, BigDecimal.ZERO,
                LocalDateTime.now(), java.util.List.of(), "Evt", LocalDateTime.now(), "Loc", LocalDateTime.now(), null);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(mapped);

        OrderResponse resp = service.trackOrder(new TrackOrderRequest("ORD-321", "guest@example.com"));
        assertEquals("ORD-321", resp.orderNumber());
    }

    @Test
    void downloadTicket_returnsPdfBytes() {
        Order order = new Order();
        order.setId(5L);
        order.setOrderNumber("ORD-999");

        Attendee attendee = new Attendee();
        attendee.setId(10L);
        attendee.setOrder(order);
        when(attendeeRepository.findById(10L)).thenReturn(Optional.of(attendee));

        Ticket ticket = new Ticket();
        ticket.setId(20L);
        ticket.setTicketCode("TKT-XYZ");
        when(ticketRepository.findByAttendeeId(10L)).thenReturn(Optional.of(ticket));

        byte[] bytes = service.downloadTicket("ORD-999", 10L);
        // starts with %PDF
        assertArrayEquals("%PDF-1.4".getBytes(), java.util.Arrays.copyOf(bytes, 8));
    }

    @Test
    void downloadTicketByEventAndEmail_returnsPdfBytes() {
        Attendee attendee = new Attendee();
        attendee.setId(10L);
        when(attendeeRepository.findByEmailAndEventId("guest@example.com", 77L))
                .thenReturn(Optional.of(attendee));

        Ticket ticket = new Ticket();
        ticket.setId(20L);
        ticket.setTicketCode("TKT-ABC");
        when(ticketRepository.findByAttendeeId(10L)).thenReturn(Optional.of(ticket));

        byte[] bytes = service.downloadTicketByEventAndEmail(77L, "guest@example.com");
        assertArrayEquals("%PDF-1.4".getBytes(), java.util.Arrays.copyOf(bytes, 8));
    }
}
