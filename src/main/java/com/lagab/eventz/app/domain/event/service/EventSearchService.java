package com.lagab.eventz.app.domain.event.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lagab.eventz.app.domain.event.dto.EventSearchDTO;
import com.lagab.eventz.app.domain.event.dto.EventSummaryDTO;
import com.lagab.eventz.app.domain.event.mapper.EventMapper;
import com.lagab.eventz.app.domain.event.model.Event;
import com.lagab.eventz.app.domain.event.repository.EventRepository;
import com.lagab.eventz.app.domain.event.specification.EventSpecifications;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventSearchService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    //private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public Page<EventSummaryDTO> searchEvents(EventSearchDTO searchDTO, Pageable pageable) {
        Specification<Event> spec = EventSpecifications.withCriteria(searchDTO);
        Page<Event> events = eventRepository.findAll(spec, pageable);
        return events.map(eventMapper::toSummaryDto).map(this::enrichEventSummaryDTO);
    }

    @Transactional(readOnly = true)
    public Page<EventSummaryDTO> findUpcomingFreeEvents(Pageable pageable) {
        Specification<Event> spec = EventSpecifications.publicEvents()
                                                       .and(EventSpecifications.isFree(true))
                                                       .and(EventSpecifications.startsAfter(java.time.LocalDateTime.now()));

        Page<Event> events = eventRepository.findAll(spec, pageable);
        return events.map(eventMapper::toSummaryDto).map(this::enrichEventSummaryDTO);
    }

    @Transactional(readOnly = true)
    public Page<EventSummaryDTO> findEventsByOrganizer(Long organizerId, Pageable pageable) {
        Specification<Event> spec = EventSpecifications.publicEvents()
                                                       .and(EventSpecifications.hasOrganizer(organizerId));

        Page<Event> events = eventRepository.findAll(spec, pageable);
        return events.map(eventMapper::toSummaryDto).map(this::enrichEventSummaryDTO);
    }

    @Transactional(readOnly = true)
    public Page<EventSummaryDTO> findUpcomingEventsByOrganizer(Long organizerId, Pageable pageable) {
        Specification<Event> spec = EventSpecifications.publicEvents()
                                                       .and(EventSpecifications.hasOrganizer(organizerId))
                                                       .and(EventSpecifications.upcomingEvents());

        Page<Event> events = eventRepository.findAll(spec, pageable);
        return events.map(eventMapper::toSummaryDto).map(this::enrichEventSummaryDTO);
    }

    @Transactional(readOnly = true)
    public Page<EventSummaryDTO> findEventsWithAvailableTickets(Pageable pageable) {
        Specification<Event> spec = EventSpecifications.publicEvents()
                                                       .and(EventSpecifications.hasAvailableTickets());

        Page<Event> events = eventRepository.findAll(spec, pageable);
        return events.map(eventMapper::toSummaryDto).map(this::enrichEventSummaryDTO);
    }

    private EventSummaryDTO enrichEventSummaryDTO(EventSummaryDTO dto) {
        /*Double averageRating = reviewRepository.calculateAverageRating(dto.id());
        Long reviewCount = reviewRepository.countByEventId(dto.id());*/
        Double averageRating = 5.0;
        Long reviewCount = 0L;

        // Calculate available tickets
        Integer availableTickets = calculateAvailableTickets(dto.id());

        return new EventSummaryDTO(
                dto.id(), dto.name(), dto.summary(), dto.startDate(),
                dto.endDate(), dto.status(), dto.type(), dto.imageUrl(),
                dto.isPublic(), dto.isFree(), dto.currency(), dto.city(),
                dto.country(), averageRating, reviewCount, availableTickets
        );
    }

    private Integer calculateAvailableTickets(Long eventId) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null || event.getTicketTypes() == null) {
            return 0;
        }

        return event.getTicketTypes().stream()
                    .mapToInt(tt -> {
                        if (tt.getQuantityAvailable() == null)
                            return 0;
                        int sold = tt.getQuantitySold() != null ? tt.getQuantitySold() : 0;
                        return Math.max(0, tt.getQuantityAvailable() - sold);
                    })
                    .sum();
    }
}
