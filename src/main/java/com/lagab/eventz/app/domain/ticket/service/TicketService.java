package com.lagab.eventz.app.domain.ticket.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.lagab.eventz.app.common.exception.BusinessException;
import com.lagab.eventz.app.common.exception.ResourceNotFoundException;
import com.lagab.eventz.app.domain.order.model.Order;
import com.lagab.eventz.app.domain.order.model.OrderItem;
import com.lagab.eventz.app.domain.ticket.entity.Attendee;
import com.lagab.eventz.app.domain.ticket.entity.Ticket;
import com.lagab.eventz.app.domain.ticket.entity.TicketStatus;
import com.lagab.eventz.app.domain.ticket.repository.TicketRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final AttendeeService attendeeService;

    public List<Ticket> generateTicketsForOrder(Order order) {
        List<Ticket> tickets = new ArrayList<>();

        // Get unassigned attendees from the order
        List<Attendee> unassignedAttendees = attendeeService.findUnassignedAttendees(order.getId());

        int attendeeIndex = 0;

        for (OrderItem orderItem : order.getOrderItems()) {
            for (int i = 0; i < orderItem.getQuantity(); i++) {
                Ticket ticket = new Ticket();
                ticket.setTicketCode(generateTicketCode());
                ticket.setQrCode(generateQRCode());
                ticket.setOrder(order);
                ticket.setEvent(order.getEvent());
                ticket.setTicketType(orderItem.getTicketType());
                ticket.setStatus(TicketStatus.VALID);
                ticket.setCheckedIn(false);
                ticket.setCreatedAt(LocalDateTime.now());

                // Associate an existing attendee if any remain
                if (attendeeIndex < unassignedAttendees.size()) {
                    Attendee attendee = unassignedAttendees.get(attendeeIndex++);
                    ticket.setAttendee(attendee);
                }

                tickets.add(ticket);
            }
        }

        return ticketRepository.saveAll(tickets);
    }

    public Ticket checkInTicket(String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                                        .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        if (ticket.getStatus() != TicketStatus.VALID) {
            throw new BusinessException("This ticket is not valid");
        }

        if (Boolean.TRUE.equals(ticket.getCheckedIn())) {
            throw new BusinessException("This ticket has already been scanned");
        }

        ticket.setCheckedIn(true);
        ticket.setCheckInTime(LocalDateTime.now());

        return ticketRepository.save(ticket);
    }

    private String generateTicketCode() {
        return "TKT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private String generateQRCode() {
        return UUID.randomUUID().toString();
    }
}
