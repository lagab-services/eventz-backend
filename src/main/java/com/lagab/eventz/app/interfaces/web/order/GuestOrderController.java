package com.lagab.eventz.app.interfaces.web.order;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lagab.eventz.app.domain.order.dto.OrderResponse;
import com.lagab.eventz.app.domain.order.dto.TrackOrderRequest;
import com.lagab.eventz.app.domain.order.service.GuestOrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/guest/orders")
@RequiredArgsConstructor
@Validated
@Tag(name = "Guest Orders", description = "Order management API for guests (without authentication)")
public class GuestOrderController {

    private final GuestOrderService guestOrderService;

    @Operation(
            summary = "Track an order",
            description = "Allows a guest to track the status of an order by providing order number and email"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order found successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request - missing data or incorrect format"),
            @ApiResponse(responseCode = "404", description = "Order not found with the provided information")
    })
    @PostMapping("/track")
    public ResponseEntity<OrderResponse> trackOrder(
            @Parameter(description = "Order tracking request details", required = true)
            @Valid @RequestBody TrackOrderRequest request) {
        return ResponseEntity.ok(guestOrderService.trackOrder(request));
    }

    @Operation(
            summary = "Download a PDF ticket",
            description = "Downloads the PDF ticket for a specific attendee of an order"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PDF file generated successfully",
                    content = @Content(mediaType = "application/pdf")),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "404", description = "Ticket not found for the provided identifiers")
    })
    @GetMapping("/{orderNumber}/attendees/{attendeeId}/ticket")
    public ResponseEntity<byte[]> downloadTicket(
            @Parameter(description = "Order number", example = "CMD-123456", required = true)
            @PathVariable String orderNumber,

            @Parameter(description = "Attendee ID", example = "123", required = true)
            @PathVariable Long attendeeId) {
        byte[] pdf = guestOrderService.downloadTicket(orderNumber, attendeeId);
        return ResponseEntity.ok()
                             .contentType(MediaType.APPLICATION_PDF)
                             .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ticket-" + attendeeId + ".pdf")
                             .body(pdf);
    }

    @Operation(
            summary = "Download ticket by event and email",
            description = "Downloads the PDF ticket for a specific event using the attendee's email"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PDF file generated successfully",
                    content = @Content(mediaType = "application/pdf")),
            @ApiResponse(responseCode = "400", description = "Invalid or missing email"),
            @ApiResponse(responseCode = "404", description = "Ticket not found for the provided event and email")
    })
    @GetMapping("/events/{eventId}/ticket")
    public ResponseEntity<byte[]> downloadTicketByEventAndEmail(
            @Parameter(description = "Event ID", example = "456", required = true)
            @PathVariable Long eventId,

            @Parameter(description = "Attendee email", example = "attendee@example.com", required = true)
            @RequestParam @NotBlank @Email String email) {
        byte[] pdf = guestOrderService.downloadTicketByEventAndEmail(eventId, email);
        return ResponseEntity.ok()
                             .contentType(MediaType.APPLICATION_PDF)
                             .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ticket-event-" + eventId + ".pdf")
                             .body(pdf);
    }
}
