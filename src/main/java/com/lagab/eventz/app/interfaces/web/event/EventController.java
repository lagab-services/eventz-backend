package com.lagab.eventz.app.interfaces.web.event;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lagab.eventz.app.common.dto.PageResponse;
import com.lagab.eventz.app.domain.event.dto.EventDTO;
import com.lagab.eventz.app.domain.event.dto.EventSearchDTO;
import com.lagab.eventz.app.domain.event.dto.EventSummaryDTO;
import com.lagab.eventz.app.domain.event.model.EventStatus;
import com.lagab.eventz.app.domain.event.model.EventType;
import com.lagab.eventz.app.domain.event.service.EventSearchService;
import com.lagab.eventz.app.domain.event.service.EventService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Events", description = "API for managing and searching events")
public class EventController {

    private final EventService eventService;
    private final EventSearchService eventSearchService;

    @Operation(summary = "Search events with filters",
            description = "Search events with various filters including keyword, type, status, location and date range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved events"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters provided")
    })
    @GetMapping("")
    public ResponseEntity<PageResponse<EventSummaryDTO>> searchEvents(
            @Parameter(description = "Search keyword") @RequestParam(required = false) String keyword,
            @Parameter(description = "Event type filter") @RequestParam(required = false) EventType type,
            @Parameter(description = "Event status filter") @RequestParam(required = false) EventStatus status,
            @Parameter(description = "City filter") @RequestParam(required = false) String city,
            @Parameter(description = "Start date filter (ISO format)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date filter (ISO format)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Free events filter") @RequestParam(required = false) Boolean isFree,
            @Parameter(description = "Latitude for location search") @RequestParam(required = false) Double latitude,
            @Parameter(description = "Longitude for location search") @RequestParam(required = false) Double longitude,
            @Parameter(description = "Radius in kilometers for location search") @RequestParam(required = false) Double radius,
            @Parameter(description = "Organizer ID filter") @RequestParam(required = false) Long organizerId,
            @Parameter(description = "Organization ID filter") @RequestParam(required = false) String organizationId,
            @Parameter(description = "Pagination and sorting parameters") @PageableDefault(sort = "startDate") Pageable pageable) {

        log.trace("GET /api/v1/events - Searching events with keyword: {}, type: {}, status: {}, city: {}",
                keyword, type, status, city);

        EventSearchDTO searchDTO = new EventSearchDTO(
                keyword, type, status, city, startDate, endDate, isFree,
                latitude, longitude, radius, organizerId, organizationId
        );

        Page<EventSummaryDTO> events = eventSearchService.searchEvents(searchDTO, pageable);
        return ResponseEntity.ok(PageResponse.of(events));
    }

    @Operation(summary = "Get event by ID", description = "Retrieve detailed information about a specific event")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved event"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EventDTO> getEventById(
            @Parameter(description = "ID of the event to retrieve", required = true)
            @PathVariable Long id) {
        log.trace("GET /api/v1/events/{} - Fetching event by ID", id);
        EventDTO event = eventService.getEventById(id);
        return ResponseEntity.ok(event);
    }

    @Operation(summary = "Get upcoming events", description = "Retrieve a list of upcoming published events")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved upcoming events")
    @GetMapping("/upcoming")
    public ResponseEntity<PageResponse<EventSummaryDTO>> getUpcomingEvents(
            @Parameter(description = "Pagination and sorting parameters")
            @PageableDefault(sort = "startDate") Pageable pageable) {
        log.trace("GET /api/v1/events/upcoming - Fetching upcoming events");

        EventSearchDTO searchDTO = new EventSearchDTO(
                null, null, EventStatus.PUBLISHED, null, LocalDateTime.now(), null, null,
                null, null, null, null, null
        );

        Page<EventSummaryDTO> events = eventSearchService.searchEvents(searchDTO, pageable);
        return ResponseEntity.ok(PageResponse.of(events));
    }

    @Operation(summary = "Get upcoming events by organizer",
            description = "Retrieve upcoming published events organized by a specific organizer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved events"),
            @ApiResponse(responseCode = "404", description = "Organizer not found")
    })
    @GetMapping("/upcoming/{organizerId}")
    public ResponseEntity<PageResponse<EventSummaryDTO>> getUpcomingEventsByOrganizer(
            @Parameter(description = "ID of the organizer", required = true)
            @PathVariable Long organizerId,
            @Parameter(description = "Pagination and sorting parameters")
            @PageableDefault(sort = "startDate") Pageable pageable) {
        log.trace("GET /api/v1/events/organizer/{}/upcoming - Fetching upcoming events by organizer", organizerId);

        Page<EventSummaryDTO> events = eventSearchService.findUpcomingEventsByOrganizer(organizerId, pageable);

        return ResponseEntity.ok(PageResponse.of(events));
    }

    @Operation(summary = "Get events by organizer",
            description = "Retrieve all events organized by a specific organizer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved events"),
            @ApiResponse(responseCode = "404", description = "Organizer not found")
    })
    @GetMapping("/organizer/{organizerId}")
    public ResponseEntity<PageResponse<EventSummaryDTO>> getEventsByOrganizer(
            @Parameter(description = "ID of the organizer", required = true)
            @PathVariable Long organizerId,
            @Parameter(description = "Pagination and sorting parameters")
            @PageableDefault(sort = "startDate") Pageable pageable) {
        log.trace("GET /api/v1/events/organizer/{} - Fetching events by organizer", organizerId);
        Page<EventSummaryDTO> events = eventSearchService.findEventsByOrganizer(organizerId, pageable);
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
            @PathVariable String city,
            @Parameter(description = "Pagination and sorting parameters")
            @PageableDefault(sort = "startDate") Pageable pageable) {
        log.trace("GET /api/v1/events/city/{} - Fetching events by city", city);

        EventSearchDTO searchDTO = new EventSearchDTO(
                null, null, EventStatus.PUBLISHED, city, null, null, null,
                null, null, null, null, null
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
            @PathVariable EventType type,
            @Parameter(description = "Pagination and sorting parameters")
            @PageableDefault(sort = "startDate") Pageable pageable) {
        log.trace("GET /api/v1/events/type/{} - Fetching events by type", type);

        EventSearchDTO searchDTO = new EventSearchDTO(
                null, type, EventStatus.PUBLISHED, null, null, null, null,
                null, null, null, null, null
        );

        Page<EventSummaryDTO> events = eventSearchService.searchEvents(searchDTO, pageable);
        return ResponseEntity.ok(PageResponse.of(events));
    }

    @GetMapping("/free")
    public ResponseEntity<PageResponse<EventSummaryDTO>> getFreeEvents(
            @PageableDefault(sort = "startDate") Pageable pageable) {
        log.trace("GET /api/v1/events/free - Fetching free events");

        EventSearchDTO searchDTO = new EventSearchDTO(
                null, null, EventStatus.PUBLISHED, null, null, null, true,
                null, null, null, null, null
        );

        Page<EventSummaryDTO> events = eventSearchService.searchEvents(searchDTO, pageable);
        return ResponseEntity.ok(PageResponse.of(events));
    }

    @GetMapping("/available-tickets")
    public ResponseEntity<PageResponse<EventSummaryDTO>> getEventsWithAvailableTickets(
            @PageableDefault(sort = "startDate") Pageable pageable) {
        log.trace("GET /api/v1/events/available-tickets - Fetching events with available tickets");
        Page<EventSummaryDTO> events = eventSearchService.findEventsWithAvailableTickets(pageable);
        return ResponseEntity.ok(PageResponse.of(events));
    }

    @GetMapping("/upcoming-free")
    public ResponseEntity<Page<EventSummaryDTO>> getUpcomingFreeEvents(
            @PageableDefault(sort = "startDate") Pageable pageable) {
        log.trace("GET /api/v1/events/upcoming-free - Fetching upcoming free events");
        Page<EventSummaryDTO> events = eventSearchService.findUpcomingFreeEvents(pageable);
        return ResponseEntity.ok(events);
    }

    @Operation(summary = "Get nearby events",
            description = "Retrieve published events near specified coordinates within given radius")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved events"),
            @ApiResponse(responseCode = "400", description = "Invalid location parameters")
    })
    @GetMapping("/nearby")
    public ResponseEntity<PageResponse<EventSummaryDTO>> getNearbyEvents(
            @Parameter(description = "Latitude for location search", required = true)
            @RequestParam Double latitude,
            @Parameter(description = "Longitude for location search", required = true)
            @RequestParam Double longitude,
            @Parameter(description = "Search radius in kilometers", example = "10.0")
            @RequestParam(defaultValue = "10.0") Double radius,
            @Parameter(description = "Optional event type filter")
            @RequestParam(required = false) EventType type,
            @Parameter(description = "Optional free events filter")
            @RequestParam(required = false) Boolean isFree,
            @Parameter(description = "Pagination and sorting parameters")
            @PageableDefault(sort = "startDate") Pageable pageable) {

        log.trace("GET /api/v1/events/nearby - Fetching events near lat: {}, lng: {}, radius: {}km",
                latitude, longitude, radius);

        EventSearchDTO searchDTO = new EventSearchDTO(
                null, type, EventStatus.PUBLISHED, null, LocalDateTime.now(), null, isFree,
                latitude, longitude, radius, null, null
        );

        Page<EventSummaryDTO> events = eventSearchService.searchEvents(searchDTO, pageable);
        return ResponseEntity.ok(PageResponse.of(events));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<PageResponse<EventSummaryDTO>> getEventsByStatus(
            @PathVariable EventStatus status,
            @PageableDefault(sort = "startDate") Pageable pageable) {
        log.trace("GET /api/v1/events/status/{} - Fetching events by status", status);

        EventSearchDTO searchDTO = new EventSearchDTO(
                null, null, status, null, null, null, null,
                null, null, null, null, null
        );

        Page<EventSummaryDTO> events = eventSearchService.searchEvents(searchDTO, pageable);
        return ResponseEntity.ok(PageResponse.of(events));
    }

    @Operation(summary = "Count events by organizer",
            description = "Get the count of events organized by a specific organizer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved count"),
            @ApiResponse(responseCode = "404", description = "Organizer not found")
    })
    @GetMapping("/organizer/{organizerId}/count")
    public ResponseEntity<Long> countEventsByOrganizer(
            @Parameter(description = "ID of the organizer", required = true)
            @PathVariable Long organizerId) {
        log.trace("GET /api/v1/events/organizer/{}/count - Counting events by organizer", organizerId);
        long count = eventService.countEventsByOrganizer(organizerId);
        return ResponseEntity.ok(count);
    }
}
