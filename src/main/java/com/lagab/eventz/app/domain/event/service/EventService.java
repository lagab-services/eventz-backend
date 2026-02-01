package com.lagab.eventz.app.domain.event.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lagab.eventz.app.common.exception.UnauthorizedException;
import com.lagab.eventz.app.domain.event.dto.CreateEventDTO;
import com.lagab.eventz.app.domain.event.dto.EventDTO;
import com.lagab.eventz.app.domain.event.dto.UpdateEventDTO;
import com.lagab.eventz.app.domain.event.exception.EventNotFoundException;
import com.lagab.eventz.app.domain.event.mapper.EventMapper;
import com.lagab.eventz.app.domain.event.model.Event;
import com.lagab.eventz.app.domain.event.model.EventStatus;
import com.lagab.eventz.app.domain.event.repository.EventRepository;
import com.lagab.eventz.app.domain.org.model.Organization;
import com.lagab.eventz.app.domain.org.service.OrganizationService;
import com.lagab.eventz.app.domain.user.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final AddressService addressService;
    private final OrganizationService organizationService;

    @Transactional(readOnly = true)
    public EventDTO getEventById(Long id) {
        Event event = findEventById(id);
        EventDTO dto = eventMapper.toDto(event);
        return enrichEventDTO(dto);
    }

    @Transactional(readOnly = true)
    public Page<EventDTO> getAllEvents(Pageable pageable) {
        Page<Event> events = eventRepository.findAll(pageable);
        return events.map(eventMapper::toDto)
                     .map(this::enrichEventDTO);
    }

    public EventDTO createEvent(String orgId, CreateEventDTO createEventDTO) {
        log.debug("Creating new event: {}", createEventDTO.name());

        User organizer = getCurrentUser();

        Organization organization = organizationService.getOrganizationById(orgId);

        Event event = eventMapper.toEntity(createEventDTO);
        event.setOrganizer(organizer);
        event.setOrganization(organization);

        if (createEventDTO.address() != null) {
            event.setAddress(addressService.createAddress(createEventDTO.address(), event));
        }

        Event savedEvent = eventRepository.save(event);
        log.debug("Event created successfully with ID: {}", savedEvent.getId());

        return enrichEventDTO(eventMapper.toDto(savedEvent));
    }

    public EventDTO updateEvent(Long id, UpdateEventDTO updateEventDTO) {
        log.debug("Updating event with ID: {}", id);
        Event event = findEventById(id);

        eventMapper.updateEntity(updateEventDTO, event);

        if (updateEventDTO.address() != null) {
            addressService.updateAddress(event.getAddress(), updateEventDTO.address());
        }

        Event updatedEvent = eventRepository.save(event);
        log.debug("Event updated successfully with ID: {}", updatedEvent.getId());

        return enrichEventDTO(eventMapper.toDto(updatedEvent));
    }

    private static User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (User) principal;
    }

    public void deleteEvent(Long id) {
        log.debug("Deleting event with ID: {}", id);

        Event event = findEventById(id);

        User currentUser = getCurrentUser();
        // Check if user is authorized to delete this event
        if (!event.getOrganizer().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to delete this event");
        }

        eventRepository.delete(event);
        log.debug("Event deleted successfully with ID: {}", id);
    }

    public EventDTO publishEvent(Long id) {
        log.debug("Publishing event with ID: {}", id);

        Event event = findEventById(id);

        // Validate event can be published
        validateEventForPublishing(event);

        event.setStatus(EventStatus.PUBLISHED);
        Event publishedEvent = eventRepository.save(event);

        log.debug("Event published successfully with ID: {}", publishedEvent.getId());

        return enrichEventDTO(eventMapper.toDto(publishedEvent));
    }

    public EventDTO cancelEvent(Long id) {
        log.debug("Cancelling event with ID: {}", id);

        Event event = findEventById(id);

        User currentUser = getCurrentUser();
        // Check if user is authorized to cancel this event
        if (!event.getOrganizer().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to delete this event");
        }

        event.setStatus(EventStatus.CANCELLED);
        Event cancelledEvent = eventRepository.save(event);

        log.debug("Event cancelled successfully with ID: {}", cancelledEvent.getId());

        return enrichEventDTO(eventMapper.toDto(cancelledEvent));
    }

    @Transactional(readOnly = true)
    public long countEventsByOrganizer(Long organizerId) {
        return eventRepository.countByOrganizerId(organizerId);
    }

    @Transactional(readOnly = true)
    public long countEventsByOrganization(String orgId) {
        return eventRepository.countByOrganizationId(orgId);
    }

    public String getOrganizationIdByEventId(Long eventId) {
        return eventRepository.findOrganizationIdByEventId(eventId)
                              .orElseThrow(() -> new EventNotFoundException("Event not found with id: " + eventId));
    }

    // Private helper methods
    private Event findEventById(Long id) {
        return eventRepository.findById(id)
                              .orElseThrow(() -> new EventNotFoundException("Event not found with ID: " + id));
    }

    private EventDTO enrichEventDTO(EventDTO dto) {
        /*Double averageRating = reviewRepository.calculateAverageRating(dto.id());
        Long reviewCount = reviewRepository.countByEventId(dto.id());*/
        Double averageRating = 5.0;
        Long reviewCount = 0L;

        return new EventDTO(
                dto.id(), dto.name(), dto.description(), dto.summary(),
                dto.surtitle(), dto.subtitle(), dto.startDate(), dto.endDate(),
                dto.registrationStart(), dto.registrationEnd(), dto.status(),
                dto.type(), dto.imageUrl(), dto.website(), dto.maxAttendees(),
                dto.isPublic(), dto.isFree(), dto.currency(), dto.createdAt(),
                dto.updatedAt(), dto.organizerId(), dto.organizerName(),
                dto.organizationId(), dto.organizationName(),
                dto.address(), dto.ticketTypes(), dto.ticketCategories(), averageRating, reviewCount
        );
    }

    private void validateEventForPublishing(Event event) {
        if (event.getStartDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Cannot publish event that starts in the past");
        }

        if (event.getEndDate().isBefore(event.getStartDate())) {
            throw new IllegalStateException("Event end date must be after start date");
        }

        if (event.getAddress() == null) {
            throw new IllegalStateException("Event must have an address to be published");
        }

        if (event.getTicketTypes() == null || event.getTicketTypes().isEmpty()) {
            throw new IllegalStateException("Event must have at least one ticket type to be published");
        }
    }
}
