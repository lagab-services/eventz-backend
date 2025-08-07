package com.lagab.eventz.app.interfaces.web.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lagab.eventz.app.common.dto.PageResponse;
import com.lagab.eventz.app.domain.event.dto.CreateEventDTO;
import com.lagab.eventz.app.domain.event.dto.EventDTO;
import com.lagab.eventz.app.domain.event.dto.EventSearchDTO;
import com.lagab.eventz.app.domain.event.dto.EventSummaryDTO;
import com.lagab.eventz.app.domain.event.dto.UpdateEventDTO;
import com.lagab.eventz.app.domain.event.model.EventStatus;
import com.lagab.eventz.app.domain.event.model.EventType;
import com.lagab.eventz.app.domain.event.service.EventSearchService;
import com.lagab.eventz.app.domain.event.service.EventService;
import com.lagab.eventz.app.interfaces.web.org.annotation.RequireOrganizationPermission;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}/events")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Organization Events", description = "API for managing organization events")
public class OrganizationEventController {

    private final EventService eventService;
    private final EventSearchService eventSearchService;

    @Operation(summary = "Get event by ID", description = "Retrieve detailed information about a specific event")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved event"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @GetMapping("/{eventId}")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<EventDTO> getEventById(
            @Parameter(description = "ID of the event to retrieve", required = true)
            @PathVariable Long eventId) {
        log.trace("GET /api/v1/events/{} - Fetching event by ID", eventId);
        EventDTO event = eventService.getEventById(eventId);
        return ResponseEntity.ok(event);
    }

    @Operation(summary = "Get upcoming events by organization",
            description = "Retrieve upcoming published events organized by a specific organizer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved events"),
            @ApiResponse(responseCode = "404", description = "Organizer not found")
    })
    @GetMapping("/upcoming")
    public ResponseEntity<PageResponse<EventSummaryDTO>> getUpcomingEventsByOrganization(
            @Parameter(description = "ID of the organizer", required = true)
            @PathVariable String orgId,
            @Parameter(description = "Pagination and sorting parameters")
            @PageableDefault(sort = "startDate") Pageable pageable) {
        log.trace("GET /api/v1/events/organizer/{}/upcoming - Fetching upcoming events by organization", orgId);

        Page<EventSummaryDTO> events = eventSearchService.findUpcomingEventsByOrganization(orgId, pageable);

        return ResponseEntity.ok(PageResponse.of(events));
    }

    @Operation(summary = "Advanced event search",
            description = "Search events using comprehensive criteria in request body")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved events"),
            @ApiResponse(responseCode = "400", description = "Invalid search criteria")
    })
    @PostMapping("/search")
    public ResponseEntity<PageResponse<EventSummaryDTO>> searchEventsAdvanced(
            @Parameter(description = "Search criteria object", required = true)
            @Valid @RequestBody EventSearchDTO searchDTO,
            @Parameter(description = "Pagination and sorting parameters")
            @PageableDefault(sort = "startDate") Pageable pageable) {

        log.trace("POST /api/v1/events/search - Advanced search with criteria: {}", searchDTO);
        Page<EventSummaryDTO> events = eventSearchService.searchEvents(searchDTO, pageable);
        return ResponseEntity.ok(PageResponse.of(events));
    }

    @Operation(summary = "Get events by city",
            description = "Retrieve published events in a specific city")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved events"),
            @ApiResponse(responseCode = "400", description = "Invalid city parameter")
    })
    @GetMapping("/city/{city}")
    public ResponseEntity<PageResponse<EventSummaryDTO>> getEventsByCity(
            @Parameter(description = "City name to filter by", required = true)
            @PathVariable String orgId,
            @PathVariable String city,
            @Parameter(description = "Pagination and sorting parameters")
            @PageableDefault(sort = "startDate") Pageable pageable) {
        log.trace("GET /api/v1/events/city/{} - Fetching events by city", city);

        EventSearchDTO searchDTO = new EventSearchDTO(
                null, null, EventStatus.PUBLISHED, city, null, null, null,
                null, null, null, null, orgId
        );

        Page<EventSummaryDTO> events = eventSearchService.searchEvents(searchDTO, pageable);
        return ResponseEntity.ok(PageResponse.of(events));
    }

    @Operation(summary = "Get events by type",
            description = "Retrieve published events of a specific type")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved events"),
            @ApiResponse(responseCode = "400", description = "Invalid event type")
    })
    @GetMapping("/type/{type}")
    public ResponseEntity<PageResponse<EventSummaryDTO>> getEventsByType(
            @Parameter(description = "Event type to filter by", required = true)
            @PathVariable String orgId,
            @PathVariable EventType type,
            @Parameter(description = "Pagination and sorting parameters")
            @PageableDefault(sort = "startDate") Pageable pageable) {
        log.trace("GET /api/v1/events/type/{} - Fetching events by type", type);

        EventSearchDTO searchDTO = new EventSearchDTO(
                null, type, EventStatus.PUBLISHED, null, null, null, null,
                null, null, null, null, orgId
        );

        Page<EventSummaryDTO> events = eventSearchService.searchEvents(searchDTO, pageable);
        return ResponseEntity.ok(PageResponse.of(events));
    }

    @GetMapping("/free")
    public ResponseEntity<PageResponse<EventSummaryDTO>> getFreeEvents(
            @PathVariable String orgId,
            @PageableDefault(sort = "startDate") Pageable pageable) {
        log.trace("GET /api/v1/events/free - Fetching free events");

        EventSearchDTO searchDTO = new EventSearchDTO(
                null, null, EventStatus.PUBLISHED, null, null, null, true,
                null, null, null, null, orgId
        );

        Page<EventSummaryDTO> events = eventSearchService.searchEvents(searchDTO, pageable);
        return ResponseEntity.ok(PageResponse.of(events));
    }

    @GetMapping("/status/{status}")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<PageResponse<EventSummaryDTO>> getEventsByStatus(
            @PathVariable String orgId,
            @PathVariable EventStatus status,
            @PageableDefault(sort = "startDate") Pageable pageable) {
        log.trace("GET /api/v1/events/status/{} - Fetching events by status", status);

        EventSearchDTO searchDTO = new EventSearchDTO(
                null, null, status, null, null, null, null,
                null, null, null, null, orgId
        );

        Page<EventSummaryDTO> events = eventSearchService.searchEvents(searchDTO, pageable);
        return ResponseEntity.ok(PageResponse.of(events));
    }

    // CRUD
    @Operation(summary = "Create a new event", description = "Create a new event with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Event created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    @RequireOrganizationPermission(permission = "EVENT_CREATE")
    public ResponseEntity<EventDTO> createEvent(
            @PathVariable String orgId,
            @Parameter(description = "Event creation data", required = true)
            @Valid @RequestBody CreateEventDTO createEventDTO) {
        log.trace("POST /api/v1/events - Creating new event: {}", createEventDTO.name());
        EventDTO createdEvent = eventService.createEvent(orgId, createEventDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEvent);
    }

    @Operation(summary = "Update an event", description = "Update an existing event with new information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @PutMapping("/{eventId}")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<EventDTO> updateEvent(
            @Parameter(description = "ID of the event to update", required = true)
            @PathVariable Long eventId,
            @Parameter(description = "Updated event data", required = true)
            @Valid @RequestBody UpdateEventDTO updateEventDTO) {

        log.trace("PUT /api/v1/events/{} - Updating event", eventId);
        EventDTO updatedEvent = eventService.updateEvent(eventId, updateEventDTO);
        return ResponseEntity.ok(updatedEvent);
    }

    @Operation(summary = "Delete an event", description = "Delete an existing event")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Event deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @DeleteMapping("/{eventId}")
    @RequireOrganizationPermission(permission = "EVENT_DELETE")
    public ResponseEntity<Void> deleteEvent(
            @Parameter(description = "ID of the event to delete", required = true)
            @PathVariable Long eventId) {
        log.trace("DELETE /api/v1/events/{} - Deleting event", eventId);
        eventService.deleteEvent(eventId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Publish an event",
            description = "Change an event's status to published")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event published successfully"),
            @ApiResponse(responseCode = "404", description = "Event not found"),
            @ApiResponse(responseCode = "409", description = "Event cannot be published")
    })
    @PutMapping("/{eventId}/publish")
    @RequireOrganizationPermission(permission = "EVENT_PUBLISH")
    public ResponseEntity<EventDTO> publishEvent(
            @Parameter(description = "ID of the event to publish", required = true)
            @PathVariable Long eventId) {
        log.trace("PUT /api/v1/events/{}/publish - Publishing event", eventId);
        EventDTO publishedEvent = eventService.publishEvent(eventId);
        return ResponseEntity.ok(publishedEvent);
    }

    @Operation(summary = "Cancel an event",
            description = "Change an event's status to cancelled")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event cancelled successfully"),
            @ApiResponse(responseCode = "404", description = "Event not found"),
            @ApiResponse(responseCode = "409", description = "Event cannot be cancelled")
    })
    @PutMapping("/{eventId}/cancel")
    @RequireOrganizationPermission(permission = "EVENT_PUBLISH")
    public ResponseEntity<EventDTO> cancelEvent(
            @Parameter(description = "ID of the event to cancel", required = true)
            @PathVariable Long eventId) {

        log.trace("PUT /api/v1/events/{}/cancel - Cancelling event", eventId);
        EventDTO cancelledEvent = eventService.cancelEvent(eventId);
        return ResponseEntity.ok(cancelledEvent);
    }
}
