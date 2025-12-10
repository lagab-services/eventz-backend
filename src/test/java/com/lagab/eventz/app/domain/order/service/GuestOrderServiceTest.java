package com.lagab.eventz.app.domain.order.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lagab.eventz.app.common.exception.ResourceNotFoundException;
import com.lagab.eventz.app.domain.order.dto.OrderResponse;
import com.lagab.eventz.app.domain.order.dto.TrackOrderRequest;
import com.lagab.eventz.app.domain.order.mapper.OrderMapper;
import com.lagab.eventz.app.domain.order.model.Order;
import com.lagab.eventz.app.domain.order.model.OrderStatus;
import com.lagab.eventz.app.domain.order.repository.OrderRepository;
import com.lagab.eventz.app.domain.ticket.entity.Attendee;
import com.lagab.eventz.app.domain.ticket.repository.AttendeeRepository;
import com.lagab.eventz.app.domain.ticket.service.AttendeeService;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
    AttendeeService attendeeService;

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
        order.setTickets(new ArrayList<>());

        when(orderRepository.findByOrderNumberAndBillingEmailIgnoreCase("ORD-321", "guest@example.com"))
                .thenReturn(Optional.of(order));

        OrderResponse mapped = new OrderResponse(1L, "ORD-321", OrderStatus.PAID, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO,
                LocalDateTime.now(), java.util.List.of(), "Evt", "evt_E1", LocalDateTime.now(), LocalDateTime.now().plusDays(1), "Loc", "Addr",
                LocalDateTime.now(), null, new ArrayList<>());
        when(orderMapper.toResponse(any(Order.class))).thenReturn(mapped);

        OrderResponse resp = service.trackOrder(new TrackOrderRequest("ORD-321", "guest@example.com"));
        assertEquals("ORD-321", resp.orderNumber());
    }

    @Test
    void downloadTicket_success_whenAttendeeBelongsToOrder() {
        Long attendeeId = 42L;
        String orderNumber = "ORD-999";
        // attendee with order
        Attendee attendee = new Attendee();
        attendee.setId(attendeeId);
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        attendee.setOrder(order);

        when(attendeeRepository.findById(attendeeId)).thenReturn(Optional.of(attendee));
        byte[] pdf = new byte[] { 1, 2, 3 };
        when(attendeeService.generateTicketPdf(attendeeId)).thenReturn(pdf);

        byte[] result = service.downloadTicket(orderNumber, attendeeId);

        assertArrayEquals(pdf, result);
        verify(attendeeRepository).findById(attendeeId);
        verify(attendeeService).generateTicketPdf(attendeeId);
        verifyNoMoreInteractions(attendeeRepository, attendeeService);
    }

    @Test
    void downloadTicket_throws_whenAttendeeNotFound() {
        Long attendeeId = 77L;
        when(attendeeRepository.findById(attendeeId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.downloadTicket("ORD-1", attendeeId));
        assertEquals("Attendee not found", ex.getMessage());

        verify(attendeeRepository).findById(attendeeId);
        verifyNoInteractions(attendeeService);
    }

    @Test
    void downloadTicket_throws_whenAttendeeDoesNotBelongToOrder_nullOrder() {
        Long attendeeId = 5L;
        Attendee attendee = new Attendee();
        attendee.setId(attendeeId);
        attendee.setOrder(null);
        when(attendeeRepository.findById(attendeeId)).thenReturn(Optional.of(attendee));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.downloadTicket("ORD-ABC", attendeeId));
        assertEquals("Attendee does not belong to this order", ex.getMessage());
        verify(attendeeRepository).findById(attendeeId);
        verifyNoInteractions(attendeeService);
    }

    @Test
    void downloadTicket_throws_whenOrderNumberMismatch() {
        Long attendeeId = 6L;
        Attendee attendee = new Attendee();
        attendee.setId(attendeeId);
        Order order = new Order();
        order.setOrderNumber("ORD-OTHER");
        attendee.setOrder(order);
        when(attendeeRepository.findById(attendeeId)).thenReturn(Optional.of(attendee));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.downloadTicket("ORD-EXPECTED", attendeeId));
        assertEquals("Attendee does not belong to this order", ex.getMessage());
        verify(attendeeRepository).findById(attendeeId);
        verifyNoInteractions(attendeeService);
    }
}
