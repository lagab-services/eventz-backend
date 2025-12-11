package com.lagab.eventz.app.domain.event.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.lagab.eventz.app.common.exception.UnauthorizedException;
import com.lagab.eventz.app.domain.event.dto.CreateAddressDTO;
import com.lagab.eventz.app.domain.event.dto.CreateEventDTO;
import com.lagab.eventz.app.domain.event.dto.EventDTO;
import com.lagab.eventz.app.domain.event.dto.UpdateAddressDTO;
import com.lagab.eventz.app.domain.event.dto.UpdateEventDTO;
import com.lagab.eventz.app.domain.event.exception.EventNotFoundException;
import com.lagab.eventz.app.domain.event.mapper.EventMapper;
import com.lagab.eventz.app.domain.event.model.Address;
import com.lagab.eventz.app.domain.event.model.Event;
import com.lagab.eventz.app.domain.event.model.EventStatus;
import com.lagab.eventz.app.domain.event.model.EventType;
import com.lagab.eventz.app.domain.event.model.TicketType;
import com.lagab.eventz.app.domain.event.repository.EventRepository;
import com.lagab.eventz.app.domain.org.model.Organization;
import com.lagab.eventz.app.domain.org.service.OrganizationService;
import com.lagab.eventz.app.domain.user.model.Role;
import com.lagab.eventz.app.domain.user.model.User;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Event Service Tests")
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private AddressService addressService;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private EventService eventService;

    private static final Long EVENT_ID = 1L;
    private static final String ORG_ID = "org-123";
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    private User currentUser;
    private Event event;
    private EventDTO eventDTO;
    private Organization organization;

    @BeforeEach
    void setUp() {
        currentUser = createUser(USER_ID, "current@example.com");
        organization = createOrganization();
        event = createEvent();
        eventDTO = createEventDTO();
    }

    @Nested
    @DisplayName("Read Operations Tests")
    class ReadOperationsTests {

        @Test
        @DisplayName("Should get event by ID successfully")
        void getEventById_WithValidId_ShouldReturnEventDTO() {
            // Given
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(eventMapper.toDto(event)).thenReturn(eventDTO);

            // When
            EventDTO result = eventService.getEventById(EVENT_ID);

            // Then
            assertNotNull(result);
            assertEquals(EVENT_ID, result.id());
            assertEquals("Test Event", result.name());
            assertEquals(5.0, result.averageRating());
            assertEquals(0L, result.reviewCount());
        }

        @Test
        @DisplayName("Should throw exception when event not found")
        void getEventById_WithInvalidId_ShouldThrowException() {
            // Given
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

            // When & Then
            EventNotFoundException exception = assertThrows(
                    EventNotFoundException.class,
                    () -> eventService.getEventById(EVENT_ID)
            );
            assertEquals("Event not found with ID: " + EVENT_ID, exception.getMessage());
        }

        @Test
        @DisplayName("Should get all events with pagination")
        void getAllEvents_ShouldReturnPagedEvents() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Event> eventPage = new PageImpl<>(List.of(event));
            when(eventRepository.findAll(pageable)).thenReturn(eventPage);
            when(eventMapper.toDto(event)).thenReturn(eventDTO);

            // When
            Page<EventDTO> result = eventService.getAllEvents(pageable);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals(EVENT_ID, result.getContent().getFirst().id());
        }

        @ParameterizedTest
        @ValueSource(longs = { 0L, 1L, 5L, 100L })
        @DisplayName("Should count events by organizer")
        void countEventsByOrganizer_ShouldReturnCorrectCount(long expectedCount) {
            // Given
            when(eventRepository.countByOrganizerId(USER_ID)).thenReturn(expectedCount);

            // When
            long result = eventService.countEventsByOrganizer(USER_ID);

            // Then
            assertEquals(expectedCount, result);
        }

        @ParameterizedTest
        @ValueSource(longs = { 0L, 1L, 10L, 50L })
        @DisplayName("Should count events by organization")
        void countEventsByOrganization_ShouldReturnCorrectCount(long expectedCount) {
            // Given
            when(eventRepository.countByOrganizationId(ORG_ID)).thenReturn(expectedCount);

            // When
            long result = eventService.countEventsByOrganization(ORG_ID);

            // Then
            assertEquals(expectedCount, result);
        }

        @Test
        @DisplayName("Should get organization ID by event ID")
        void getOrganizationIdByEventId_WithValidEventId_ShouldReturnOrgId() {
            // Given
            when(eventRepository.findOrganizationIdByEventId(EVENT_ID)).thenReturn(Optional.of(ORG_ID));

            // When
            String result = eventService.getOrganizationIdByEventId(EVENT_ID);

            // Then
            assertEquals(ORG_ID, result);
        }

        @Test
        @DisplayName("Should throw exception when getting org ID for non-existent event")
        void getOrganizationIdByEventId_WithInvalidEventId_ShouldThrowException() {
            // Given
            when(eventRepository.findOrganizationIdByEventId(EVENT_ID)).thenReturn(Optional.empty());

            // When & Then
            EventNotFoundException exception = assertThrows(
                    EventNotFoundException.class,
                    () -> eventService.getOrganizationIdByEventId(EVENT_ID)
            );
            assertEquals("Event not found with id: " + EVENT_ID, exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Create Event Tests")
    class CreateEventTests {

        @Test
        @DisplayName("Should create event successfully")
        void createEvent_WithValidData_ShouldCreateEvent() {
            // Given
            CreateEventDTO createDTO = createCreateEventDTO();

            try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
                setupSecurityContext(mockedSecurityContext, currentUser);

                when(organizationService.getOrganizationById(ORG_ID)).thenReturn(organization);
                when(eventMapper.toEntity(createDTO)).thenReturn(event);
                when(eventRepository.save(event)).thenReturn(event);
                when(eventMapper.toDto(event)).thenReturn(eventDTO);

                // When
                EventDTO result = eventService.createEvent(ORG_ID, createDTO);

                // Then
                assertNotNull(result);
                assertEquals(EVENT_ID, result.id());
                assertEquals(currentUser, event.getOrganizer());
                assertEquals(organization, event.getOrganization());
                verify(eventRepository).save(event);
                verify(addressService).createAddress(createDTO.address(), event);
            }
        }

        @Test
        @DisplayName("Should create event without address")
        void createEvent_WithoutAddress_ShouldCreateEvent() {
            // Given
            CreateEventDTO createDTO = createCreateEventDTOWithoutAddress();

            try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
                setupSecurityContext(mockedSecurityContext, currentUser);

                when(organizationService.getOrganizationById(ORG_ID)).thenReturn(organization);
                when(eventMapper.toEntity(createDTO)).thenReturn(event);
                when(eventRepository.save(event)).thenReturn(event);
                when(eventMapper.toDto(event)).thenReturn(eventDTO);

                // When
                EventDTO result = eventService.createEvent(ORG_ID, createDTO);

                // Then
                assertNotNull(result);
                verify(addressService, never()).createAddress(any(), any());
            }
        }
    }

    @Nested
    @DisplayName("Update Event Tests")
    class UpdateEventTests {

        @Test
        @DisplayName("Should update event successfully")
        void updateEvent_WithValidData_ShouldUpdateEvent() {
            // Given
            UpdateEventDTO updateDTO = createUpdateEventDTO();
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(eventRepository.save(event)).thenReturn(event);
            when(eventMapper.toDto(event)).thenReturn(eventDTO);

            // When
            EventDTO result = eventService.updateEvent(EVENT_ID, updateDTO);

            // Then
            assertNotNull(result);
            verify(eventMapper).updateEntity(updateDTO, event);
            verify(addressService).updateAddress(event.getAddress(), updateDTO.address());
            verify(eventRepository).save(event);
        }

        @Test
        @DisplayName("Should update event without address")
        void updateEvent_WithoutAddress_ShouldUpdateEvent() {
            // Given
            UpdateEventDTO updateDTO = createUpdateEventDTOWithoutAddress();
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(eventRepository.save(event)).thenReturn(event);
            when(eventMapper.toDto(event)).thenReturn(eventDTO);

            // When
            EventDTO result = eventService.updateEvent(EVENT_ID, updateDTO);

            // Then
            assertNotNull(result);
            verify(addressService, never()).updateAddress(any(), any());
        }
    }

    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {

        @ParameterizedTest
        @MethodSource("authorizationScenarios")
        @DisplayName("Should handle authorization correctly for different operations")
        void authorizationTests_ShouldHandleCorrectly(
                String operation,
                Long eventOrganizerId,
                Long currentUserId,
                boolean shouldSucceed) {

            // Given
            User eventOrganizer = createUser(eventOrganizerId, "organizer@example.com");
            User user = createUser(currentUserId, "current@example.com");
            Event testEvent = createEventWithOrganizer(eventOrganizer);

            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(testEvent));

            try (MockedStatic<SecurityContextHolder> mockedSecurityContext = mockStatic(SecurityContextHolder.class)) {
                setupSecurityContext(mockedSecurityContext, user);

                if (operation.equals("delete")) {
                    if (shouldSucceed) {
                        // When & Then
                        assertDoesNotThrow(() -> eventService.deleteEvent(EVENT_ID));
                        verify(eventRepository).delete(testEvent);
                    } else {
                        // When & Then
                        assertThrows(UnauthorizedException.class, () -> eventService.deleteEvent(EVENT_ID));
                    }
                } else if (operation.equals("cancel")) {
                    lenient().when(eventRepository.save(testEvent)).thenReturn(testEvent);
                    lenient().when(eventMapper.toDto(testEvent)).thenReturn(eventDTO);

                    if (shouldSucceed) {
                        // When & Then
                        assertDoesNotThrow(() -> eventService.cancelEvent(EVENT_ID));
                        assertEquals(EventStatus.CANCELLED, testEvent.getStatus());
                    } else {
                        // When & Then
                        assertThrows(UnauthorizedException.class, () -> eventService.cancelEvent(EVENT_ID));
                    }
                }
            }
        }

        static Stream<Arguments> authorizationScenarios() {
            return Stream.of(
                    Arguments.of("delete", USER_ID, USER_ID, true),
                    Arguments.of("delete", USER_ID, OTHER_USER_ID, false),
                    Arguments.of("cancel", USER_ID, USER_ID, true),
                    Arguments.of("cancel", USER_ID, OTHER_USER_ID, false)
            );
        }
    }

    @Nested
    @DisplayName("Event Status Management Tests")
    class EventStatusManagementTests {

        @Test
        @DisplayName("Should publish event successfully")
        void publishEvent_WithValidEvent_ShouldPublishEvent() {
            // Given
            Event validEvent = createValidEventForPublishing();
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(validEvent));
            when(eventRepository.save(validEvent)).thenReturn(validEvent);
            when(eventMapper.toDto(validEvent)).thenReturn(eventDTO);

            // When
            EventDTO result = eventService.publishEvent(EVENT_ID);

            // Then
            assertNotNull(result);
            assertEquals(EventStatus.PUBLISHED, validEvent.getStatus());
            verify(eventRepository).save(validEvent);
        }

        @ParameterizedTest
        @MethodSource("invalidEventForPublishingScenarios")
        @DisplayName("Should throw exception when publishing invalid event")
        void publishEvent_WithInvalidEvent_ShouldThrowException(
                String scenario,
                Event invalidEvent,
                String expectedMessage) {

            // Given
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(invalidEvent));

            // When & Then
            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> eventService.publishEvent(EVENT_ID)
            );
            assertTrue(exception.getMessage().contains(expectedMessage),
                    "Expected message to contain: " + expectedMessage + ", but was: " + exception.getMessage());
        }

        static Stream<Arguments> invalidEventForPublishingScenarios() {
            return Stream.of(
                    Arguments.of(
                            "Past start date",
                            createEventWithPastStartDate(),
                            "Cannot publish event that starts in the past"
                    ),
                    Arguments.of(
                            "End before start",
                            createEventWithEndBeforeStart(),
                            "Event end date must be after start date"
                    ),
                    Arguments.of(
                            "No address",
                            createEventWithoutAddress(),
                            "Event must have an address to be published"
                    ),
                    Arguments.of(
                            "No ticket types",
                            createEventWithoutTicketTypes(),
                            "Event must have at least one ticket type to be published"
                    )
            );
        }

        private static Event createEventWithPastStartDate() {
            Event event = createValidEventForPublishing();
            event.setStartDate(LocalDateTime.now().minusDays(1));
            return event;
        }

        private static Event createEventWithEndBeforeStart() {
            Event event = createValidEventForPublishing();
            event.setStartDate(LocalDateTime.now().plusDays(2));
            event.setEndDate(LocalDateTime.now().plusDays(1));
            return event;
        }

        private static Event createEventWithoutAddress() {
            Event event = createValidEventForPublishing();
            event.setAddress(null);
            return event;
        }

        private static Event createEventWithoutTicketTypes() {
            Event event = createValidEventForPublishing();
            event.setTicketTypes(List.of());
            return event;
        }
    }

    @Nested
    @DisplayName("Event Status Tests")
    class EventStatusTests {

        @ParameterizedTest
        @EnumSource(EventStatus.class)
        @DisplayName("Should handle events with different statuses")
        void handleEventStatus_WithDifferentStatuses_ShouldWork(EventStatus status) {
            // Given
            Event eventWithStatus = createEventWithStatus(status);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(eventWithStatus));
            when(eventMapper.toDto(eventWithStatus)).thenReturn(eventDTO);

            // When
            EventDTO result = eventService.getEventById(EVENT_ID);

            // Then
            assertNotNull(result);
            assertEquals(EVENT_ID, result.id());
        }

        private Event createEventWithStatus(EventStatus status) {
            Event newEventWithStatus = createEvent();
            newEventWithStatus.setStatus(status);
            return newEventWithStatus;
        }
    }

    // Helper methods for creating real entities
    private static User createUser(Long id, String email) {
        return User.builder()
                   .id(id)
                   .email(email)
                   .firstName("Test")
                   .lastName("User")
                   .role(Role.USER)
                   .isActive(true)
                   .isEmailVerified(true)
                   .createdAt(LocalDateTime.now())
                   .updatedAt(LocalDateTime.now())
                   .build();
    }

    private Organization createOrganization() {
        Organization newOrganization = new Organization();
        newOrganization.setId(ORG_ID);
        newOrganization.setName("Test Organization");
        newOrganization.setSlug("test-organization");
        newOrganization.setEmail("test@example.com");
        newOrganization.setCreatedAt(LocalDateTime.now());
        newOrganization.setUpdatedAt(LocalDateTime.now());
        return newOrganization;
    }

    private Event createEvent() {
        Event createEvent = new Event();
        createEvent.setId(EVENT_ID);
        createEvent.setName("Test Event");
        createEvent.setDescription("Test Description");
        createEvent.setStartDate(LocalDateTime.now().plusDays(1));
        createEvent.setEndDate(LocalDateTime.now().plusDays(2));
        createEvent.setStatus(EventStatus.DRAFT);
        createEvent.setType(EventType.CONFERENCE);
        createEvent.setMaxAttendees(100);
        createEvent.setIsPublic(true);
        createEvent.setIsFree(false);
        createEvent.setCurrency("EUR");
        createEvent.setOrganizer(currentUser);
        createEvent.setOrganization(organization);
        createEvent.setCreatedAt(LocalDateTime.now());
        createEvent.setUpdatedAt(LocalDateTime.now());
        return createEvent;
    }

    private Event createEventWithOrganizer(User organizer) {
        Event createEvent = new Event();
        createEvent.setId(EVENT_ID);
        createEvent.setName("Test Event");
        createEvent.setDescription("Test Description");
        createEvent.setStartDate(LocalDateTime.now().plusDays(1));
        createEvent.setEndDate(LocalDateTime.now().plusDays(2));
        createEvent.setStatus(EventStatus.DRAFT);
        createEvent.setType(EventType.CONFERENCE);
        createEvent.setMaxAttendees(100);
        createEvent.setIsPublic(true);
        createEvent.setIsFree(false);
        createEvent.setCurrency("EUR");
        createEvent.setOrganizer(organizer);
        createEvent.setOrganization(organization);
        createEvent.setCreatedAt(LocalDateTime.now());
        createEvent.setUpdatedAt(LocalDateTime.now());
        return createEvent;
    }

    private static Event createValidEventForPublishing() {
        // Create a real TicketType for testing
        TicketType ticketType = new TicketType();
        ticketType.setId(1L);
        ticketType.setName("Standard");
        ticketType.setPrice(BigDecimal.valueOf(50.0));
        ticketType.setQuantityAvailable(100);

        // Create a simple address object (assuming Address class exists)
        Address address = new Address(); // Replace with actual Address creation

        Event createEvent = new Event();
        createEvent.setId(EVENT_ID);
        createEvent.setName("Valid Event");
        createEvent.setDescription("Valid Description");
        createEvent.setStartDate(LocalDateTime.now().plusDays(1));
        createEvent.setEndDate(LocalDateTime.now().plusDays(2));
        createEvent.setStatus(EventStatus.DRAFT);
        createEvent.setType(EventType.CONFERENCE);
        createEvent.setMaxAttendees(100);
        createEvent.setIsPublic(true);
        createEvent.setIsFree(false);
        createEvent.setCurrency("EUR");
        createEvent.setTicketTypes(List.of(ticketType));
        createEvent.setAddress(address);
        createEvent.setCreatedAt(LocalDateTime.now());
        createEvent.setUpdatedAt(LocalDateTime.now());
        return createEvent;
    }

    private EventDTO createEventDTO() {
        return new EventDTO(
                EVENT_ID, "Test Event", "Description", "Summary",
                "Surtitle", "Subtitle", LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2),
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), EventStatus.DRAFT,
                EventType.CONFERENCE, "image.jpg", "website.com", 100,
                true, false, "EUR", LocalDateTime.now(),
                LocalDateTime.now(), USER_ID, "Organizer Name",
                ORG_ID, "Organization Name",
                null, List.of(), List.of(), 5.0, 0L
        );
    }

    private CreateEventDTO createCreateEventDTO() {
        return new CreateEventDTO(
                "New Event", "Description", "Summary",
                "Surtitle", "Subtitle", LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2),
                LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                EventType.CONFERENCE, "image.jpg", "website.com", 100,
                true, false, "EUR",
                new CreateAddressDTO("name", "address1", "address2", "city", "state", "country", "zipCode", BigDecimal.ZERO, BigDecimal.ZERO,
                        false,
                        "") // address - replace with actual Address creation
        );
    }

    private CreateEventDTO createCreateEventDTOWithoutAddress() {
        return new CreateEventDTO(
                "New Event", "Description", "Summary",
                "Surtitle", "Subtitle",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                EventType.CONFERENCE, "image.jpg", "website.com", 100,
                true, false, "EUR",
                null // no address
        );
    }

    private UpdateEventDTO createUpdateEventDTO() {
        return new UpdateEventDTO(
                "Updated Event", "Updated Description", "Updated Summary",
                "Updated Surtitle", "Updated Subtitle", LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(3),
                LocalDateTime.now(), LocalDateTime.now().plusDays(2),
                EventStatus.PUBLISHED,
                EventType.WORKSHOP, "updated-image.jpg", "updated-website.com", 150,
                false, true, "USD",
                new UpdateAddressDTO("name", "address1", "address2", "city", "state", "country", "zipCode", BigDecimal.ZERO, BigDecimal.ZERO,
                        false,
                        "") // address - replace with actual Address creation
        );
    }

    private UpdateEventDTO createUpdateEventDTOWithoutAddress() {
        return new UpdateEventDTO(
                "Updated Event", "Updated Description", "Updated Summary",
                "Updated Surtitle", "Updated Subtitle", LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(3),
                LocalDateTime.now(), LocalDateTime.now().plusDays(2),
                EventStatus.PUBLISHED,
                EventType.WORKSHOP, "updated-image.jpg", "updated-website.com", 150,
                false, true, "USD",
                null // no address
        );
    }

    private void setupSecurityContext(MockedStatic<SecurityContextHolder> mockedSecurityContext, User user) {
        mockedSecurityContext.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
    }
}
