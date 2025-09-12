package com.lagab.eventz.app.domain.order.service;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lagab.eventz.app.common.exception.ResourceNotFoundException;
import com.lagab.eventz.app.domain.order.dto.OrderResponse;
import com.lagab.eventz.app.domain.order.dto.TrackOrderRequest;
import com.lagab.eventz.app.domain.order.mapper.OrderMapper;
import com.lagab.eventz.app.domain.order.model.Order;
import com.lagab.eventz.app.domain.order.repository.OrderRepository;
import com.lagab.eventz.app.domain.ticket.entity.Attendee;
import com.lagab.eventz.app.domain.ticket.entity.Ticket;
import com.lagab.eventz.app.domain.ticket.repository.AttendeeRepository;
import com.lagab.eventz.app.domain.ticket.repository.TicketRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GuestOrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final AttendeeRepository attendeeRepository;
    private final TicketRepository ticketRepository;

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

        Ticket ticket = ticketRepository.findByAttendeeId(attendeeId)
                                        .orElseThrow(() -> new ResourceNotFoundException("Ticket not found for attendee"));

        return buildSimplePdf(ticket);
    }

    @Transactional(readOnly = true)
    public byte[] downloadTicketByEventAndEmail(Long eventId, String email) {
        Attendee attendee = attendeeRepository.findByEmailAndEventId(email, eventId)
                                              .orElseThrow(() -> new ResourceNotFoundException("Attendee not found for provided event/email"));

        Ticket ticket = ticketRepository.findByAttendeeId(attendee.getId())
                                        .orElseThrow(() -> new ResourceNotFoundException("Ticket not found for attendee"));

        return buildSimplePdf(ticket);
    }

    private byte[] buildSimplePdf(Ticket ticket) {
        // Minimal placeholder PDF content; replace with real PDF generation later.
        String pdfText = "%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n1 0 obj<<>>endobj\n" +
                "2 0 obj<< /Length 44 >>stream\nBT /F1 12 Tf 72 720 Td (Ticket: " + ticket.getTicketCode() + ") Tj ET\nendstream endobj\n" +
                "3 0 obj<< /Type /Page /Parent 4 0 R /MediaBox [0 0 612 792] /Contents 2 0 R >>endobj\n" +
                "4 0 obj<< /Type /Pages /Kids [3 0 R] /Count 1 >>endobj\n" +
                "5 0 obj<< /Type /Catalog /Pages 4 0 R >>endobj\n" +
                "xref\n0 6\n0000000000 65535 f \n0000000015 00000 n \n0000000060 00000 n \n0000000175 00000 n \n0000000260 00000 n \n0000000315 00000 n \n"
                +
                "trailer<< /Size 6 /Root 5 0 R >>\nstartxref\n360\n%%EOF";
        return pdfText.getBytes(StandardCharsets.UTF_8);
    }
}
