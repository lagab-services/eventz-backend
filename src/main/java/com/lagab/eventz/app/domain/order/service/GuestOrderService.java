package com.lagab.eventz.app.domain.order.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lagab.eventz.app.common.exception.ResourceNotFoundException;
import com.lagab.eventz.app.domain.order.dto.OrderResponse;
import com.lagab.eventz.app.domain.order.dto.TrackOrderRequest;
import com.lagab.eventz.app.domain.order.mapper.OrderMapper;
import com.lagab.eventz.app.domain.order.model.Order;
import com.lagab.eventz.app.domain.order.repository.OrderRepository;
import com.lagab.eventz.app.domain.ticket.entity.Attendee;
import com.lagab.eventz.app.domain.ticket.repository.AttendeeRepository;
import com.lagab.eventz.app.domain.ticket.service.AttendeeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GuestOrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final AttendeeRepository attendeeRepository;
    private final AttendeeService attendeeService;

    @Transactional(readOnly = true)
    public OrderResponse trackOrder(TrackOrderRequest request) {
        Order order = orderRepository
                .findByOrderNumberAndBillingEmailIgnoreCase(request.orderNumber(), request.email())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found for provided number/email"));
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public byte[] downloadTicket(String orderNumber, Long attendeeId) {
        Attendee attendee = attendeeRepository.findById(attendeeId)
                                              .orElseThrow(() -> new ResourceNotFoundException("Attendee not found"));

        if (attendee.getOrder() == null || attendee.getOrder().getOrderNumber() == null
                || !attendee.getOrder().getOrderNumber().equals(orderNumber)) {
            throw new ResourceNotFoundException("Attendee does not belong to this order");
        }

        return attendeeService.generateTicketPdf(attendeeId);
    }
}
