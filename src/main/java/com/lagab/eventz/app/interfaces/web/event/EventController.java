package com.lagab.eventz.app.interfaces.web.event;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Slf4j
public class EventController {

    private final EventService eventService;
    private final EventSearchService eventSearchService;

    @GetMapping("")
    public ResponseEntity<PageResponse<EventSummaryDTO>> searchEvents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) EventType type,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Boolean isFree,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double radius,
            @RequestParam(required = false) Long organizerId,
            @PageableDefault(sort = "startDate") Pageable pageable) {

        log.trace("GET /api/v1/events - Searching events with keyword: {}, type: {}, status: {}, city: {}",
                keyword, type, status, city);

        EventSearchDTO searchDTO = new EventSearchDTO(
                keyword, type, status, city, startDate, endDate, isFree,
                latitude, longitude, radius, organizerId
        );

        Page<EventSummaryDTO> events = eventSearchService.searchEvents(searchDTO, pageable);
        return ResponseEntity.ok(PageResponse.of(events));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDTO> getEventById(@PathVariable Long id) {
        log.trace("GET /api/v1/events/{} - Fetching event by ID", id);
        EventDTO event = eventService.getEventById(id);
        return ResponseEntity.ok(event);
    }

    @GetMapping("/upcoming")
    public ResponseEntity<PageResponse<EventSummaryDTO>> getUpcomingEvents(
            @PageableDefault(sort = "startDate") Pageable pageable) {
        log.trace("GET /api/v1/events/upcoming - Fetching upcoming events");

        EventSearchDTO searchDTO = new EventSearchDTO(
                null, null, EventStatus.PUBLISHED, null, LocalDateTime.now(), null, null,
                null, null, null, null
        );

        Page<EventSummaryDTO> events = eventSearchService.searchEvents(searchDTO, pageable);
        return ResponseEntity.ok(PageResponse.of(events));
    }

    @GetMapping("/upcoming/{organizerId}")
    public ResponseEntity<PageResponse<EventSummaryDTO>> getUpcomingEventsByOrganizer(@PathVariable Long organizerId,
            @PageableDefault(sort = "startDate") Pageable pageable) {
        log.trace("GET /api/v1/events/organizer/{}/upcoming - Fetching upcoming events by organizer", organizerId);

        Page<EventSummaryDTO> events = eventSearchService.findUpcomingEventsByOrganizer(organizerId, pageable);

        return ResponseEntity.ok(PageResponse.of(events));
    }

    @GetMapping("/organizer/{organizerId}")
    public ResponseEntity<PageResponse<EventSummaryDTO>> getEventsByOrganizer(@PathVariable Long organizerId,
            @PageableDefault(sort = "startDate") Pageable pageable) {
        log.trace("GET /api/v1/events/organizer/{} - Fetching events by organizer", organizerId);
        Page<EventSummaryDTO> events = eventSearchService.findEventsByOrganizer(organizerId, pageable);
        return ResponseEntity.ok(PageResponse.of(events));
    }

    @PostMapping("/search")
    public ResponseEntity<PageResponse<EventSummaryDTO>> searchEventsAdvanced(
            @Valid @RequestBody EventSearchDTO searchDTO,
            @PageableDefault(sort = "startDate") Pageable pageable) {

        log.trace("POST /api/v1/events/search - Advanced search with criteria: {}", searchDTO);
        Page<EventSummaryDTO> events = eventSearchService.searchEvents(searchDTO, pageable);
        return ResponseEntity.ok(PageResponse.of(events));
    }

    @GetMapping("/city/{city}")
    public ResponseEntity<PageResponse<EventSummaryDTO>> getEventsByCity(
            @PathVariable String city,
            @PageableDefault(sort = "startDate") Pageable pageable) {
        log.trace("GET /api/v1/events/city/{} - Fetching events by city", city);

        EventSearchDTO searchDTO = new EventSearchDTO(
                null, null, EventStatus.PUBLISHED, city, null, null, null,
                null, null, null, null
        );

        Page<EventSummaryDTO> events = eventSearchService.searchEvents(searchDTO, pageable);
        return ResponseEntity.ok(PageResponse.of(events));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<PageResponse<EventSummaryDTO>> getEventsByType(
            @PathVariable EventType type,
            @PageableDefault(sort = "startDate") Pageable pageable) {
        log.trace("GET /api/v1/events/type/{} - Fetching events by type", type);

        EventSearchDTO searchDTO = new EventSearchDTO(
                null, type, EventStatus.PUBLISHED, null, null, null, null,
                null, null, null, null
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
                null, null, null, null
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

    @GetMapping("/nearby")
    public ResponseEntity<PageResponse<EventSummaryDTO>> getNearbyEvents(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "10.0") Double radius,
            @RequestParam(required = false) EventType type,
            @RequestParam(required = false) Boolean isFree,
            @PageableDefault(sort = "startDate") Pageable pageable) {

        log.trace("GET /api/v1/events/nearby - Fetching events near lat: {}, lng: {}, radius: {}km",
                latitude, longitude, radius);

        EventSearchDTO searchDTO = new EventSearchDTO(
                null, type, EventStatus.PUBLISHED, null, LocalDateTime.now(), null, isFree,
                latitude, longitude, radius, null
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
                null, null, null, null
        );

        Page<EventSummaryDTO> events = eventSearchService.searchEvents(searchDTO, pageable);
        return ResponseEntity.ok(PageResponse.of(events));
    }

    @GetMapping("/organizer/{organizerId}/count")
    public ResponseEntity<Long> countEventsByOrganizer(@PathVariable Long organizerId) {
        log.trace("GET /api/v1/events/organizer/{}/count - Counting events by organizer", organizerId);
        long count = eventService.countEventsByOrganizer(organizerId);
        return ResponseEntity.ok(count);
    }

    // Endpoints de gestion des événements (CRUD)
    @PostMapping
    public ResponseEntity<EventDTO> createEvent(@Valid @RequestBody CreateEventDTO createEventDTO) {
        log.trace("POST /api/v1/events - Creating new event: {}", createEventDTO.name());
        EventDTO createdEvent = eventService.createEvent(createEventDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEvent);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventDTO> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventDTO updateEventDTO) {

        log.trace("PUT /api/v1/events/{} - Updating event", id);
        EventDTO updatedEvent = eventService.updateEvent(id, updateEventDTO);
        return ResponseEntity.ok(updatedEvent);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        log.trace("DELETE /api/v1/events/{} - Deleting event", id);
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/publish")
    public ResponseEntity<EventDTO> publishEvent(@PathVariable Long id) {
        log.trace("PUT /api/v1/events/{}/publish - Publishing event", id);
        EventDTO publishedEvent = eventService.publishEvent(id);
        return ResponseEntity.ok(publishedEvent);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<EventDTO> cancelEvent(@PathVariable Long id) {

        log.trace("PUT /api/v1/events/{}/cancel - Cancelling event", id);
        EventDTO cancelledEvent = eventService.cancelEvent(id);
        return ResponseEntity.ok(cancelledEvent);
    }
}
