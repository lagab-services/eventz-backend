package com.lagab.eventz.app.interfaces.web.ticket;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lagab.eventz.app.common.dto.PageResponse;
import com.lagab.eventz.app.domain.ticket.dto.AttendeeInfo;
import com.lagab.eventz.app.domain.ticket.dto.AttendeeResponse;
import com.lagab.eventz.app.domain.ticket.dto.AttendeeStatistics;
import com.lagab.eventz.app.domain.ticket.dto.TransferTicketRequest;
import com.lagab.eventz.app.domain.ticket.service.AttendeeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/attendees")
@RequiredArgsConstructor
public class AttendeeController {

    private final AttendeeService attendeeService;

    //Todo: add search

    @GetMapping("/event/{eventId}")
    public ResponseEntity<PageResponse<AttendeeResponse>> getEventAttendees(@PathVariable Long eventId, Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(attendeeService.getEventAttendees(eventId, pageable)));
    }

    @GetMapping("/event/{eventId}/statistics")
    public ResponseEntity<AttendeeStatistics> getEventStatistics(@PathVariable Long eventId) {
        AttendeeStatistics stats = attendeeService.getEventStatistics(eventId);
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/{attendeeId}/check-in")
    public ResponseEntity<Void> checkInAttendee(
            @PathVariable Long attendeeId,
            @RequestParam String checkedInBy) {
        attendeeService.checkInAttendee(attendeeId, checkedInBy);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{attendeeId}")
    public ResponseEntity<AttendeeResponse> updateAttendee(
            @PathVariable Long attendeeId,
            @Valid @RequestBody AttendeeInfo request) {
        AttendeeResponse updated = attendeeService.updateAttendee(attendeeId, request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{attendeeId}/transfer")
    public ResponseEntity<AttendeeResponse> transferTicket(
            @PathVariable Long attendeeId,
            @Valid @RequestBody TransferTicketRequest request) {
        AttendeeResponse transferred = attendeeService.transferTicket(attendeeId, request);
        return ResponseEntity.ok(transferred);
    }

    /*@PostMapping("/event/{eventId}/bulk-check-in")
    public ResponseEntity<BulkCheckInResponse> bulkCheckIn(
            @PathVariable Long eventId,
            @RequestBody BulkCheckInRequest request) {
        BulkCheckInResponse response = attendeeService.bulkCheckIn(eventId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/event/{eventId}/send-reminders")
    public ResponseEntity<Void> sendEventReminders(@PathVariable Long eventId) {
        attendeeService.sendEventReminders(eventId);
        return ResponseEntity.ok().build();
    }*/

   /* @GetMapping("/{attendeeId}/ticket")
    public ResponseEntity<byte[]> downloadTicket(@PathVariable Long attendeeId) {
        byte[] ticketPdf = attendeeService.generateTicketPdf(attendeeId);
        return ResponseEntity.ok()
                             .header("Content-Type", "application/pdf")
                             .header("Content-Disposition", "attachment; filename=ticket.pdf")
                             .body(ticketPdf);
    }*/
}

