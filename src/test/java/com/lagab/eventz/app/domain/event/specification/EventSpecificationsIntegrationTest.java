package com.lagab.eventz.app.domain.event.specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.domain.Specification;

import com.lagab.eventz.app.domain.event.dto.EventSearchDTO;
import com.lagab.eventz.app.domain.event.model.Address;
import com.lagab.eventz.app.domain.event.model.Event;
import com.lagab.eventz.app.domain.event.model.EventStatus;
import com.lagab.eventz.app.domain.event.model.EventType;
import com.lagab.eventz.app.domain.event.repository.EventRepository;
import com.lagab.eventz.app.domain.org.model.Organization;
import com.lagab.eventz.app.domain.org.repository.OrganizationRepository;
import com.lagab.eventz.app.domain.user.model.User;
import com.lagab.eventz.app.domain.user.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@DisplayName("EventSpecifications Integration Tests")
class EventSpecificationsIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrganizationRepository organizationRepository;

    private Event testEvent1;
    private Event testEvent2;
    private Event testEvent3;
    private User testOrganizer;
    private Organization testOrganization;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        eventRepository.deleteAll();

        // Create and save organizer first if it's a separate entity
        testOrganizer = createTestOrganizer();
        testOrganizer = userRepository.save(testOrganizer);

        testOrganization = createTestOrganization();
        testOrganization = organizationRepository.save(testOrganization);

        // Create test events
        testEvent1 = createTestEvent("Spring Conference", "Learn about Spring",
                EventType.CONFERENCE, EventStatus.PUBLISHED, "Paris", true, true, testOrganizer, testOrganization);

        testEvent2 = createTestEvent("Java Workshop", "Hands-on Java coding",
                EventType.WORKSHOP, EventStatus.DRAFT, "Lyon", false, true, testOrganizer, testOrganization);

        testEvent3 = createTestEvent("Tech Meetup", "Networking event",
                EventType.SEMINAR, EventStatus.PUBLISHED, "Paris", true, false, testOrganizer, testOrganization);

        // Save using repository
        testEvent1 = eventRepository.save(testEvent1);
        testEvent2 = eventRepository.save(testEvent2);
        testEvent3 = eventRepository.save(testEvent3);

        // Force synchronization with database
        eventRepository.flush();
        entityManager.flush();
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        eventRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    private User createTestOrganizer() {
        User organizer = new User();
        organizer.setFirstName("Test");
        organizer.setLastName("Organizer");
        organizer.setEmail("test@example.com");
        organizer.setPassword("password");
        return organizer;
    }

    private Organization createTestOrganization() {
        Organization organizer = new Organization();
        organizer.setName("Test Organization");
        organizer.setSlug("test-organization");
        organizer.setEmail("test@example.com");
        return organizer;
    }

    @Test
    @DisplayName("Should find events by keyword")
    void shouldFindEventsByKeyword() {
        // Given
        Specification<Event> spec = EventSpecifications.hasKeyword("Spring");

        // When
        List<Event> results = eventRepository.findAll(spec);

        // Then
        assertEquals(1, results.size());
        assertEquals("Spring Conference", results.get(0).getName());
    }

    @Test
    @DisplayName("Should find events by type")
    void shouldFindEventsByType() {
        // Given
        Specification<Event> spec = EventSpecifications.hasType(EventType.CONFERENCE);

        // When
        List<Event> results = eventRepository.findAll(spec);

        // Then
        assertEquals(1, results.size());
        assertEquals(EventType.CONFERENCE, results.get(0).getType());
    }

    @Test
    @DisplayName("Should find events by status")
    void shouldFindEventsByStatus() {
        // Given
        Specification<Event> spec = EventSpecifications.hasStatus(EventStatus.PUBLISHED);

        // When
        List<Event> results = eventRepository.findAll(spec);

        // Then
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(event -> event.getStatus() == EventStatus.PUBLISHED));
    }

    @Test
    @DisplayName("Should find only public events")
    void shouldFindOnlyPublicEvents() {
        // Given
        Specification<Event> spec = EventSpecifications.isPublic();

        // When
        List<Event> results = eventRepository.findAll(spec);

        // Then
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(Event::getIsPublic));
    }

    @Test
    @DisplayName("Should find only free events")
    void shouldFindOnlyFreeEvents() {
        // Given
        Specification<Event> spec = EventSpecifications.isFree(true);

        // When
        List<Event> results = eventRepository.findAll(spec);

        // Then
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(Event::getIsFree));
    }

    @Test
    @DisplayName("Should combine multiple specifications")
    void shouldCombineMultipleSpecifications() {
        // Given
        Specification<Event> spec = EventSpecifications.publicEvents()
                                                       .and(EventSpecifications.hasStatus(EventStatus.PUBLISHED));

        // When
        List<Event> results = eventRepository.findAll(spec);

        // Then
        assertEquals(1, results.size());
        Event result = results.get(0);
        assertEquals("Paris", result.getAddress().getCity());
        assertEquals(EventStatus.PUBLISHED, result.getStatus());
        assertTrue(result.getIsPublic());
    }

    @Test
    @DisplayName("Should work with complex search criteria")
    void shouldWorkWithComplexSearchCriteria() {
        // Given
        EventSearchDTO searchDTO = new EventSearchDTO(
                "Spring",
                EventType.CONFERENCE,
                EventStatus.PUBLISHED,
                null,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30),
                true,
                null,
                null,
                null,
                null,
                null
        );

        Specification<Event> spec = EventSpecifications.withCriteria(searchDTO);

        // When
        List<Event> results = eventRepository.findAll(spec);

        // Then
        assertEquals(1, results.size());
        Event result = results.get(0);
        assertEquals("Spring Conference", result.getName());
        assertEquals(EventType.CONFERENCE, result.getType());
        assertEquals(EventStatus.PUBLISHED, result.getStatus());
        assertEquals("Paris", result.getAddress().getCity());
        assertTrue(result.getIsFree());
        assertTrue(result.getIsPublic());
    }

    @Test
    @DisplayName("Should return empty list when no events match criteria")
    void shouldReturnEmptyListWhenNoEventsMatchCriteria() {
        // Given
        Specification<Event> spec = EventSpecifications.hasKeyword("NonExistentKeyword");

        // When
        List<Event> results = eventRepository.findAll(spec);

        // Then
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should find upcoming events")
    void shouldFindUpcomingEvents() {
        // Given
        // Create an event in the future
        Event futureEvent = createTestEvent("Future Event", "Event in future",
                EventType.CONFERENCE, EventStatus.PUBLISHED, "Paris", true, true, testOrganizer, testOrganization);
        futureEvent.setStartDate(LocalDateTime.now().plusDays(10));
        entityManager.persistAndFlush(futureEvent);

        Specification<Event> spec = EventSpecifications.upcomingEvents();

        // When
        List<Event> results = eventRepository.findAll(spec);

        // Then
        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(event -> event.getStartDate().isAfter(LocalDateTime.now())));
    }

    @Test
    @DisplayName("Should find published events")
    void shouldFindPublishedEvents() {
        // Given
        Specification<Event> spec = EventSpecifications.publishedEvents();

        // When
        List<Event> results = eventRepository.findAll(spec);

        // Then
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(event -> event.getStatus() == EventStatus.PUBLISHED));
    }

    private Event createTestEvent(String name, String description, EventType type,
            EventStatus status, String city, boolean isFree, boolean isPublic, User organizer, Organization organization) {
        Event event = new Event();
        event.setName(name);
        event.setDescription(description);
        event.setType(type);
        event.setStatus(status);
        event.setIsFree(isFree);
        event.setIsPublic(isPublic);
        event.setStartDate(LocalDateTime.now().plusDays(1));
        event.setEndDate(LocalDateTime.now().plusDays(2));

        // Create address
        Address address = new Address();
        address.setCity(city);
        address.setLatitude(BigDecimal.valueOf(48.8566)); // Paris coordinates as default
        address.setLongitude(BigDecimal.valueOf(2.3522));
        event.setAddress(address);

        // Create organizer
        event.setOrganizer(organizer);
        event.setOrganization(organization);

        return event;
    }
}

