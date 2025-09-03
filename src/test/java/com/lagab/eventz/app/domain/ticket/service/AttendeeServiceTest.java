package com.lagab.eventz.app.domain.ticket.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.lagab.eventz.app.common.exception.BusinessException;
import com.lagab.eventz.app.common.exception.ResourceNotFoundException;
import com.lagab.eventz.app.common.exception.ValidationException;
import com.lagab.eventz.app.domain.event.model.Event;
import com.lagab.eventz.app.domain.event.model.EventCustomField;
import com.lagab.eventz.app.domain.event.model.FieldType;
import com.lagab.eventz.app.domain.event.model.TicketType;
import com.lagab.eventz.app.domain.event.repository.EventCustomFieldRepository;
import com.lagab.eventz.app.domain.order.model.Order;
import com.lagab.eventz.app.domain.ticket.dto.AttendeeInfo;
import com.lagab.eventz.app.domain.ticket.dto.AttendeeResponse;
import com.lagab.eventz.app.domain.ticket.dto.AttendeeSearchCriteria;
import com.lagab.eventz.app.domain.ticket.dto.AttendeeStatistics;
import com.lagab.eventz.app.domain.ticket.dto.TransferTicketRequest;
import com.lagab.eventz.app.domain.ticket.entity.Attendee;
import com.lagab.eventz.app.domain.ticket.entity.AttendeeCustomField;
import com.lagab.eventz.app.domain.ticket.entity.CheckInStatus;
import com.lagab.eventz.app.domain.ticket.entity.Ticket;
import com.lagab.eventz.app.domain.ticket.entity.TicketStatus;
import com.lagab.eventz.app.domain.ticket.repository.AttendeeRepository;
import com.lagab.eventz.app.domain.user.model.User;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttendeeService Tests")
class AttendeeServiceTest {

    @Mock
    private AttendeeRepository attendeeRepository;

    @Mock
    private EventCustomFieldRepository eventCustomFieldRepository;

    @InjectMocks
    private AttendeeService attendeeService;

    private Event testEvent;
    private Order testOrder;
    private Ticket testTicket;
    private TicketType testTicketType;
    private Attendee testAttendee;
    private AttendeeInfo testAttendeeInfo;
    private EventCustomField testCustomField;
    private AttendeeCustomField testAttendeeCustomField;

    @BeforeEach
    void setUp() {
        // Setup test user
        User testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("organizer@example.com");

        // Setup test event
        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Test Event");
        testEvent.setStartDate(LocalDateTime.now().plusDays(30));
        testEvent.setEndDate(LocalDateTime.now().plusDays(30).plusHours(8));

        // Setup test ticket type
        testTicketType = new TicketType();
        testTicketType.setId(1L);
        testTicketType.setName("Standard");
        testTicketType.setEvent(testEvent);

        // Setup test order
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setOrderNumber("ORD-123456");
        testOrder.setUser(testUser);
        testOrder.setEvent(testEvent);

        // Setup test ticket
        testTicket = new Ticket();
        testTicket.setId(1L);
        testTicket.setTicketCode("TKT-123456789ABC");
        testTicket.setQrCode("qr-code-123");
        testTicket.setStatus(TicketStatus.VALID);
        testTicket.setOrder(testOrder);
        testTicket.setTicketType(testTicketType);
        testTicket.setEvent(testEvent);

        // Setup test attendee
        testAttendee = new Attendee();
        testAttendee.setId(1L);
        testAttendee.setFirstName("John");
        testAttendee.setLastName("Doe");
        testAttendee.setEmail("john@example.com");
        testAttendee.setTicket(testTicket);
        testAttendee.setOrder(testOrder);
        testAttendee.setEvent(testEvent);
        testAttendee.setCheckInStatus(CheckInStatus.NOT_CHECKED_IN);
        testAttendee.setCreatedAt(LocalDateTime.now());
        testAttendee.setUpdatedAt(LocalDateTime.now());
        testAttendee.setCustomFields(new ArrayList<>());

        // Setup test custom field
        testCustomField = new EventCustomField();
        testCustomField.setId(1L);
        testCustomField.setFieldName("dietary_requirements");
        testCustomField.setFieldLabel("Dietary Requirements");
        testCustomField.setFieldType(FieldType.TEXT);
        testCustomField.setRequired(true);
        testCustomField.setEvent(testEvent);

        // Setup test attendee custom field
        testAttendeeCustomField = new AttendeeCustomField();
        testAttendeeCustomField.setId(1L);
        testAttendeeCustomField.setFieldName("dietary_requirements");
        testAttendeeCustomField.setFieldValue("Vegetarian");
        testAttendeeCustomField.setAttendee(testAttendee);
        testAttendeeCustomField.setEventField(testCustomField);

        List<AttendeeCustomField> mutableCustomFields = new ArrayList<>();
        mutableCustomFields.add(testAttendeeCustomField);
        testAttendee.setCustomFields(mutableCustomFields);

        // Setup test attendee info
        Map<String, String> customFields = new HashMap<>();
        customFields.put("dietary_requirements", "Vegetarian");

        testAttendeeInfo = new AttendeeInfo(
                "John",
                "Doe",
                "john@example.com",
                1L,
                customFields
        );
    }

    @Nested
    @DisplayName("Create Attendee Tests")
    class CreateAttendeeTests {

        @Test
        @DisplayName("Should create attendee successfully with custom fields")
        void shouldCreateAttendeeSuccessfullyWithCustomFields() {
            // Given
            when(eventCustomFieldRepository.findRequiredFieldsByEventAndTicketType(1L, 1L))
                    .thenReturn(List.of(testCustomField));
            when(attendeeRepository.save(any(Attendee.class))).thenReturn(testAttendee);

            // When
            attendeeService.createAttendee(testAttendeeInfo, testOrder, testTicket);

            // Then
            ArgumentCaptor<Attendee> attendeeCaptor = ArgumentCaptor.forClass(Attendee.class);
            verify(attendeeRepository).save(attendeeCaptor.capture());

            Attendee capturedAttendee = attendeeCaptor.getValue();
            assertEquals("John", capturedAttendee.getFirstName());
            assertEquals("Doe", capturedAttendee.getLastName());
            assertEquals("john@example.com", capturedAttendee.getEmail());
            assertEquals(testTicket, capturedAttendee.getTicket());
            assertEquals(testOrder, capturedAttendee.getOrder());
            assertEquals(testEvent, capturedAttendee.getEvent());

            verify(eventCustomFieldRepository).findRequiredFieldsByEventAndTicketType(1L, 1L);
        }

        @Test
        @DisplayName("Should create attendee without custom fields")
        void shouldCreateAttendeeWithoutCustomFields() {
            // Given
            AttendeeInfo infoWithoutCustomFields = new AttendeeInfo(
                    "Jane", "Smith", "jane@example.com", 1L, null
            );

            when(eventCustomFieldRepository.findRequiredFieldsByEventAndTicketType(1L, 1L))
                    .thenReturn(new ArrayList<>());
            when(attendeeRepository.save(any(Attendee.class))).thenReturn(testAttendee);

            // When
            attendeeService.createAttendee(infoWithoutCustomFields, testOrder, testTicket);

            // Then
            verify(attendeeRepository).save(any(Attendee.class));
            verify(eventCustomFieldRepository).findRequiredFieldsByEventAndTicketType(1L, 1L);
        }

        @Test
        @DisplayName("Should throw validation exception when required field is missing")
        void shouldThrowValidationExceptionWhenRequiredFieldIsMissing() {
            // Given
            AttendeeInfo infoWithoutRequiredField = new AttendeeInfo(
                    "Jane", "Smith", "jane@example.com", 1L, new HashMap<>()
            );

            when(eventCustomFieldRepository.findRequiredFieldsByEventAndTicketType(1L, 1L))
                    .thenReturn(List.of(testCustomField));

            // When & Then
            ValidationException exception = assertThrows(ValidationException.class,
                    () -> attendeeService.createAttendee(infoWithoutRequiredField, testOrder, testTicket));

            assertEquals("Required field missing: Dietary Requirements", exception.getMessage());
            verify(attendeeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw validation exception when required field is empty")
        void shouldThrowValidationExceptionWhenRequiredFieldIsEmpty() {
            // Given
            Map<String, String> customFieldsWithEmptyValue = new HashMap<>();
            customFieldsWithEmptyValue.put("dietary_requirements", "   ");

            AttendeeInfo infoWithEmptyField = new AttendeeInfo(
                    "Jane", "Smith", "jane@example.com", 1L, customFieldsWithEmptyValue
            );

            when(eventCustomFieldRepository.findRequiredFieldsByEventAndTicketType(1L, 1L))
                    .thenReturn(List.of(testCustomField));

            // When & Then
            ValidationException exception = assertThrows(ValidationException.class,
                    () -> attendeeService.createAttendee(infoWithEmptyField, testOrder, testTicket));

            assertEquals("Required field missing: Dietary Requirements", exception.getMessage());
        }

        @Test
        @DisplayName("Should handle multiple custom fields")
        void shouldHandleMultipleCustomFields() {
            // Given
            EventCustomField additionalField = new EventCustomField();
            additionalField.setId(2L);
            additionalField.setFieldName("company");
            additionalField.setFieldLabel("Company");
            additionalField.setRequired(false);

            Map<String, String> multipleCustomFields = new HashMap<>();
            multipleCustomFields.put("dietary_requirements", "Vegetarian");
            multipleCustomFields.put("company", "Tech Corp");

            AttendeeInfo infoWithMultipleFields = new AttendeeInfo(
                    "Jane", "Smith", "jane@example.com", 1L, multipleCustomFields
            );

            when(eventCustomFieldRepository.findRequiredFieldsByEventAndTicketType(1L, 1L))
                    .thenReturn(List.of(testCustomField));
            when(attendeeRepository.save(any(Attendee.class))).thenAnswer(invocation -> {
                Attendee savedAttendee = invocation.getArgument(0);
                // Initialize custom fields list if null to avoid NPE
                if (savedAttendee.getCustomFields() == null) {
                    savedAttendee.setCustomFields(new ArrayList<>());
                }
                savedAttendee.setId(2L); // Set an ID as would happen in real save
                return savedAttendee;
            });

            // When
            attendeeService.createAttendee(infoWithMultipleFields, testOrder, testTicket);

            // Then
            ArgumentCaptor<Attendee> attendeeCaptor = ArgumentCaptor.forClass(Attendee.class);
            verify(attendeeRepository).save(attendeeCaptor.capture());

            Attendee capturedAttendee = attendeeCaptor.getValue();
            assertEquals(2, capturedAttendee.getCustomFields().size());
        }
    }

    @Nested
    @DisplayName("Check In Attendee Tests")
    class CheckInAttendeeTests {

        @Test
        @DisplayName("Should check in attendee successfully")
        void shouldCheckInAttendeeSuccessfully() {
            // Given
            Long attendeeId = 1L;
            String checkedInBy = "admin@example.com";

            when(attendeeRepository.findById(attendeeId)).thenReturn(Optional.of(testAttendee));
            when(attendeeRepository.save(any(Attendee.class))).thenReturn(testAttendee);

            // When
            attendeeService.checkInAttendee(attendeeId, checkedInBy);

            // Then
            ArgumentCaptor<Attendee> attendeeCaptor = ArgumentCaptor.forClass(Attendee.class);
            verify(attendeeRepository).save(attendeeCaptor.capture());

            Attendee capturedAttendee = attendeeCaptor.getValue();
            assertEquals(CheckInStatus.CHECKED_IN, capturedAttendee.getCheckInStatus());
            assertNotNull(capturedAttendee.getCheckedInAt());
            assertEquals(checkedInBy, capturedAttendee.getCheckedInBy());
        }

        @Test
        @DisplayName("Should throw exception when attendee not found")
        void shouldThrowExceptionWhenAttendeeNotFound() {
            // Given
            Long attendeeId = 999L;
            when(attendeeRepository.findById(attendeeId)).thenReturn(Optional.empty());

            // When & Then
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> attendeeService.checkInAttendee(attendeeId, "admin@example.com"));

            assertEquals("Attendee not found", exception.getMessage());
            verify(attendeeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get Event Attendees Tests")
    class GetEventAttendeesTests {

        @Test
        @DisplayName("Should get event attendees successfully")
        void shouldGetEventAttendeesSuccessfully() {
            // Given
            Long eventId = 1L;
            Pageable pageable = PageRequest.of(0, 10);
            Page<Attendee> attendeePage = new PageImpl<>(List.of(testAttendee));

            when(attendeeRepository.findByEventId(eventId, pageable)).thenReturn(attendeePage);

            // When
            Page<AttendeeResponse> result = attendeeService.getEventAttendees(eventId, pageable);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getContent().size());

            AttendeeResponse response = result.getContent().getFirst();
            assertEquals(testAttendee.getId(), response.id());
            assertEquals(testAttendee.getFirstName(), response.firstName());
            assertEquals(testAttendee.getLastName(), response.lastName());
            assertEquals(testAttendee.getEmail(), response.email());
            assertEquals(testTicket.getTicketCode(), response.ticketNumber());
            assertEquals(testTicketType.getName(), response.ticketTypeName());
            assertEquals(testAttendee.getCheckInStatus(), response.checkInStatus());

            verify(attendeeRepository).findByEventId(eventId, pageable);
        }

        @Test
        @DisplayName("Should handle empty attendees list")
        void shouldHandleEmptyAttendeesList() {
            // Given
            Long eventId = 1L;
            Pageable pageable = PageRequest.of(0, 10);
            Page<Attendee> emptyPage = new PageImpl<>(new ArrayList<>());

            when(attendeeRepository.findByEventId(eventId, pageable)).thenReturn(emptyPage);

            // When
            Page<AttendeeResponse> result = attendeeService.getEventAttendees(eventId, pageable);

            // Then
            assertNotNull(result);
            assertTrue(result.getContent().isEmpty());
            assertEquals(0, result.getTotalElements());
        }
    }

    @Nested
    @DisplayName("Get Event Statistics Tests")
    class GetEventStatisticsTests {

        @Test
        @DisplayName("Should calculate event statistics correctly")
        void shouldCalculateEventStatisticsCorrectly() {
            // Given
            Long eventId = 1L;

            Attendee checkedInAttendee = createAttendee(2L, "Jane", "Smith", CheckInStatus.CHECKED_IN, "VIP");
            Attendee cancelledAttendee = createAttendee(3L, "Bob", "Johnson", CheckInStatus.CANCELLED, "Standard");
            Attendee notCheckedInAttendee = createAttendee(4L, "Alice", "Brown", CheckInStatus.NOT_CHECKED_IN, "VIP");

            List<Attendee> attendees = List.of(testAttendee, checkedInAttendee, cancelledAttendee, notCheckedInAttendee);

            when(attendeeRepository.findByEventId(eventId)).thenReturn(attendees);

            // When
            AttendeeStatistics result = attendeeService.getEventStatistics(eventId);

            // Then
            assertNotNull(result);
            assertEquals(4, result.totalAttendees());
            assertEquals(1, result.checkedIn()); // Only checkedInAttendee
            assertEquals(1, result.cancelled()); // Only cancelledAttendee

            Map<String, Long> attendeesByTicketType = result.attendeesByTicketType();
            assertEquals(2, attendeesByTicketType.get("Standard")); // testAttendee + cancelledAttendee
            assertEquals(2, attendeesByTicketType.get("VIP")); // checkedInAttendee + notCheckedInAttendee

            verify(attendeeRepository).findByEventId(eventId);
        }

        @Test
        @DisplayName("Should handle event with no attendees")
        void shouldHandleEventWithNoAttendees() {
            // Given
            Long eventId = 1L;
            when(attendeeRepository.findByEventId(eventId)).thenReturn(new ArrayList<>());

            // When
            AttendeeStatistics result = attendeeService.getEventStatistics(eventId);

            // Then
            assertNotNull(result);
            assertEquals(0, result.totalAttendees());
            assertEquals(0, result.checkedIn());
            assertEquals(0, result.cancelled());
            assertTrue(result.attendeesByTicketType().isEmpty());
        }

        @Test
        @DisplayName("Should handle attendees with same ticket type")
        void shouldHandleAttendeesWithSameTicketType() {
            // Given
            Long eventId = 1L;

            Attendee attendee2 = createAttendee(2L, "Jane", "Smith", CheckInStatus.CHECKED_IN, "Standard");
            Attendee attendee3 = createAttendee(3L, "Bob", "Johnson", CheckInStatus.NOT_CHECKED_IN, "Standard");

            List<Attendee> attendees = List.of(testAttendee, attendee2, attendee3);

            when(attendeeRepository.findByEventId(eventId)).thenReturn(attendees);

            // When
            AttendeeStatistics result = attendeeService.getEventStatistics(eventId);

            // Then
            assertEquals(3, result.totalAttendees());
            assertEquals(1, result.checkedIn());
            assertEquals(0, result.cancelled());
            assertEquals(3, result.attendeesByTicketType().get("Standard"));
        }
    }

    @Nested
    @DisplayName("Find Unassigned Attendees Tests")
    class FindUnassignedAttendeesTests {

        @Test
        @DisplayName("Should find unassigned attendees successfully")
        void shouldFindUnassignedAttendeesSuccessfully() {
            // Given
            Long orderId = 1L;
            Attendee unassignedAttendee = new Attendee();
            unassignedAttendee.setId(2L);
            unassignedAttendee.setFirstName("Jane");
            unassignedAttendee.setLastName("Smith");
            unassignedAttendee.setTicket(null); // Unassigned

            List<Attendee> unassignedAttendees = List.of(unassignedAttendee);

            when(attendeeRepository.findByOrderIdAndTicketIsNull(orderId)).thenReturn(unassignedAttendees);

            // When
            List<Attendee> result = attendeeService.findUnassignedAttendees(orderId);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(unassignedAttendee, result.getFirst());
            verify(attendeeRepository).findByOrderIdAndTicketIsNull(orderId);
        }

        @Test
        @DisplayName("Should return empty list when no unassigned attendees")
        void shouldReturnEmptyListWhenNoUnassignedAttendees() {
            // Given
            Long orderId = 1L;
            when(attendeeRepository.findByOrderIdAndTicketIsNull(orderId)).thenReturn(new ArrayList<>());

            // When
            List<Attendee> result = attendeeService.findUnassignedAttendees(orderId);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Update Attendee Tests")
    class UpdateAttendeeTests {

        @Test
        @DisplayName("Should update attendee successfully")
        void shouldUpdateAttendeeSuccessfully() {
            // Given
            Long attendeeId = 1L;
            Map<String, String> updatedCustomFields = new HashMap<>();
            updatedCustomFields.put("dietary_requirements", "Vegan");
            updatedCustomFields.put("company", "New Company");

            AttendeeInfo updateInfo = new AttendeeInfo(
                    "John Updated",
                    "Doe Updated",
                    "john.updated@example.com",
                    1L,
                    updatedCustomFields
            );

            when(attendeeRepository.findById(attendeeId)).thenReturn(Optional.of(testAttendee));
            when(attendeeRepository.save(any(Attendee.class))).thenReturn(testAttendee);

            // When
            AttendeeResponse result = attendeeService.updateAttendee(attendeeId, updateInfo);

            // Then
            assertNotNull(result);

            ArgumentCaptor<Attendee> attendeeCaptor = ArgumentCaptor.forClass(Attendee.class);
            verify(attendeeRepository).save(attendeeCaptor.capture());

            Attendee capturedAttendee = attendeeCaptor.getValue();
            assertEquals("John Updated", capturedAttendee.getFirstName());
            assertEquals("Doe Updated", capturedAttendee.getLastName());
            assertEquals("john.updated@example.com", capturedAttendee.getEmail());
            assertNotNull(capturedAttendee.getUpdatedAt());
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent attendee")
        void shouldThrowExceptionWhenUpdatingNonExistentAttendee() {
            // Given
            Long attendeeId = 999L;
            when(attendeeRepository.findById(attendeeId)).thenReturn(Optional.empty());

            // When & Then
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> attendeeService.updateAttendee(attendeeId, testAttendeeInfo));

            assertEquals("Attendee not found", exception.getMessage());
            verify(attendeeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should update attendee without custom fields")
        void shouldUpdateAttendeeWithoutCustomFields() {
            // Given
            Long attendeeId = 1L;
            AttendeeInfo updateInfoWithoutCustomFields = new AttendeeInfo(
                    "John Updated", "Doe Updated", "john.updated@example.com", 1L, null
            );

            when(attendeeRepository.findById(attendeeId)).thenReturn(Optional.of(testAttendee));
            when(attendeeRepository.save(any(Attendee.class))).thenReturn(testAttendee);

            // When
            AttendeeResponse result = attendeeService.updateAttendee(attendeeId, updateInfoWithoutCustomFields);

            // Then
            assertNotNull(result);
            verify(attendeeRepository).save(any(Attendee.class));
        }
    }

    @Nested
    @DisplayName("Transfer Ticket Tests")
    class TransferTicketTests {

        @Test
        @DisplayName("Should transfer ticket successfully")
        void shouldTransferTicketSuccessfully() {
            // Given
            Long attendeeId = 1L;
            TransferTicketRequest transferRequest = new TransferTicketRequest(
                    "Jane", "Smith", "jane@example.com", "Transfer message"
            );

            // Event is more than 1 day away, so transfer is allowed
            testEvent.setStartDate(LocalDateTime.now().plusDays(5));

            when(attendeeRepository.findById(attendeeId)).thenReturn(Optional.of(testAttendee));
            // Mock the save method to return an attendee with initialized custom fields
            when(attendeeRepository.save(any(Attendee.class))).thenAnswer(invocation -> {
                Attendee savedAttendee = invocation.getArgument(0);
                // Initialize custom fields list if null to avoid NPE
                if (savedAttendee.getCustomFields() == null) {
                    savedAttendee.setCustomFields(new ArrayList<>());
                }
                savedAttendee.setId(2L); // Set an ID as would happen in real save
                return savedAttendee;
            });

            // When
            AttendeeResponse result = attendeeService.transferTicket(attendeeId, transferRequest);

            // Then
            assertNotNull(result);
            assertEquals("Jane", result.firstName());
            assertEquals("Smith", result.lastName());
            assertEquals("jane@example.com", result.email());

            verify(attendeeRepository).delete(testAttendee);
            verify(attendeeRepository).save(any(Attendee.class));
        }

        @Test
        @DisplayName("Should throw exception when transfer not allowed")
        void shouldThrowExceptionWhenTransferNotAllowed() {
            // Given
            Long attendeeId = 1L;
            TransferTicketRequest transferRequest = new TransferTicketRequest(
                    "Jane", "Smith", "jane@example.com", "Transfer message"
            );

            // Event is less than 1 day away, so transfer is not allowed
            testEvent.setStartDate(LocalDateTime.now().plusHours(12));

            when(attendeeRepository.findById(attendeeId)).thenReturn(Optional.of(testAttendee));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> attendeeService.transferTicket(attendeeId, transferRequest));

            assertEquals("Ticket transfer not allowed for this event", exception.getMessage());
            verify(attendeeRepository, never()).delete(any());
            verify(attendeeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when attendee not found for transfer")
        void shouldThrowExceptionWhenAttendeeNotFoundForTransfer() {
            // Given
            Long attendeeId = 999L;
            TransferTicketRequest transferRequest = new TransferTicketRequest(
                    "Jane", "Smith", "jane@example.com", "Transfer message"
            );

            when(attendeeRepository.findById(attendeeId)).thenReturn(Optional.empty());

            // When & Then
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> attendeeService.transferTicket(attendeeId, transferRequest));

            assertEquals("Attendee not found", exception.getMessage());
        }

        @Test
        @DisplayName("Should handle transfer on event start date boundary")
        void shouldHandleTransferOnEventStartDateBoundary() {
            // Given
            Long attendeeId = 1L;
            TransferTicketRequest transferRequest = new TransferTicketRequest(
                    "Jane", "Smith", "jane@example.com", "Transfer message"
            );

            // Event is exactly 1 day away (boundary case)
            testEvent.setStartDate(LocalDateTime.now().plusDays(1));

            when(attendeeRepository.findById(attendeeId)).thenReturn(Optional.of(testAttendee));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> attendeeService.transferTicket(attendeeId, transferRequest));

            assertEquals("Ticket transfer not allowed for this event", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Search Attendees Tests")
    class SearchAttendeesTests {

        @Test
        @DisplayName("Should search attendees by criteria successfully")
        void shouldSearchAttendeesByCriteriaSuccessfully() {
            // Given
            AttendeeSearchCriteria criteria = new AttendeeSearchCriteria(
                    1L, // eventId
                    "John", // name
                    "john@example.com", // email
                    CheckInStatus.NOT_CHECKED_IN, // checkInStatus
                    1L, // ticketTypeId
                    null, // checkedInAfter
                    null  // checkedInBefore
            );

            List<Attendee> searchResults = List.of(testAttendee);

            when(attendeeRepository.findByCriteria(1L, "John", "john@example.com",
                    CheckInStatus.NOT_CHECKED_IN, 1L)).thenReturn(searchResults);

            // When
            List<AttendeeResponse> result = attendeeService.searchAttendees(criteria);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());

            AttendeeResponse response = result.getFirst();
            assertEquals(testAttendee.getId(), response.id());
            assertEquals(testAttendee.getFirstName(), response.firstName());

            verify(attendeeRepository).findByCriteria(1L, "John", "john@example.com",
                    CheckInStatus.NOT_CHECKED_IN, 1L);
        }

        @Test
        @DisplayName("Should return empty list when no attendees match criteria")
        void shouldReturnEmptyListWhenNoAttendeesMatchCriteria() {
            // Given
            AttendeeSearchCriteria criteria = new AttendeeSearchCriteria(
                    1L, "NonExistent", null, null, null, null, null
            );

            when(attendeeRepository.findByCriteria(1L, "NonExistent", null, null, null))
                    .thenReturn(new ArrayList<>());

            // When
            List<AttendeeResponse> result = attendeeService.searchAttendees(criteria);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should search with partial criteria")
        void shouldSearchWithPartialCriteria() {
            // Given
            AttendeeSearchCriteria partialCriteria = new AttendeeSearchCriteria(
                    1L, null, null, CheckInStatus.CHECKED_IN, null, null, null
            );

            Attendee checkedInAttendee = createAttendee(2L, "Jane", "Smith", CheckInStatus.CHECKED_IN, "VIP");
            List<Attendee> searchResults = List.of(checkedInAttendee);

            when(attendeeRepository.findByCriteria(1L, null, null, CheckInStatus.CHECKED_IN, null))
                    .thenReturn(searchResults);

            // When
            List<AttendeeResponse> result = attendeeService.searchAttendees(partialCriteria);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(CheckInStatus.CHECKED_IN, result.getFirst().checkInStatus());
        }
    }

    @Nested
    @DisplayName("Map To Response Tests")
    class MapToResponseTests {

        @Test
        @DisplayName("Should map attendee to response correctly")
        void shouldMapAttendeeToResponseCorrectly() {
            // Given
            when(attendeeRepository.findByEventId(1L, PageRequest.of(0, 10)))
                    .thenReturn(new PageImpl<>(List.of(testAttendee)));

            // When
            Page<AttendeeResponse> result = attendeeService.getEventAttendees(1L, PageRequest.of(0, 10));

            // Then
            AttendeeResponse response = result.getContent().getFirst();
            assertEquals(testAttendee.getId(), response.id());
            assertEquals(testAttendee.getFirstName(), response.firstName());
            assertEquals(testAttendee.getLastName(), response.lastName());
            assertEquals(testAttendee.getEmail(), response.email());
            assertEquals(testTicket.getTicketCode(), response.ticketNumber());
            assertEquals(testTicketType.getName(), response.ticketTypeName());
            assertEquals(testAttendee.getCheckInStatus(), response.checkInStatus());

            // Verify custom fields mapping
            Map<String, String> customFields = response.customFields();
            assertEquals(1, customFields.size());
            assertEquals("Vegetarian", customFields.get("dietary_requirements"));
        }

        @Test
        @DisplayName("Should handle attendee with no custom fields")
        void shouldHandleAttendeeWithNoCustomFields() {
            // Given
            Attendee attendeeWithoutCustomFields = new Attendee();
            attendeeWithoutCustomFields.setId(2L);
            attendeeWithoutCustomFields.setFirstName("Jane");
            attendeeWithoutCustomFields.setLastName("Smith");
            attendeeWithoutCustomFields.setEmail("jane@example.com");
            attendeeWithoutCustomFields.setTicket(testTicket);
            attendeeWithoutCustomFields.setCheckInStatus(CheckInStatus.NOT_CHECKED_IN);
            attendeeWithoutCustomFields.setCustomFields(new ArrayList<>());

            when(attendeeRepository.findByEventId(1L, PageRequest.of(0, 10)))
                    .thenReturn(new PageImpl<>(List.of(attendeeWithoutCustomFields)));

            // When
            Page<AttendeeResponse> result = attendeeService.getEventAttendees(1L, PageRequest.of(0, 10));

            // Then
            AttendeeResponse response = result.getContent().getFirst();
            assertTrue(response.customFields().isEmpty());
        }

        @Test
        @DisplayName("Should handle attendee with multiple custom fields")
        void shouldHandleAttendeeWithMultipleCustomFields() {
            // Given
            AttendeeCustomField companyField = new AttendeeCustomField();
            companyField.setFieldName("company");
            companyField.setFieldValue("Tech Corp");

            AttendeeCustomField phoneField = new AttendeeCustomField();
            phoneField.setFieldName("phone");
            phoneField.setFieldValue("+1234567890");

            testAttendee.setCustomFields(List.of(testAttendeeCustomField, companyField, phoneField));

            when(attendeeRepository.findByEventId(1L, PageRequest.of(0, 10)))
                    .thenReturn(new PageImpl<>(List.of(testAttendee)));

            // When
            Page<AttendeeResponse> result = attendeeService.getEventAttendees(1L, PageRequest.of(0, 10));

            // Then
            AttendeeResponse response = result.getContent().get(0);
            Map<String, String> customFields = response.customFields();
            assertEquals(3, customFields.size());
            assertEquals("Vegetarian", customFields.get("dietary_requirements"));
            assertEquals("Tech Corp", customFields.get("company"));
            assertEquals("+1234567890", customFields.get("phone"));
        }
    }

    @Nested
    @DisplayName("Custom Fields Validation Tests")
    class CustomFieldsValidationTests {

        @Test
        @DisplayName("Should validate multiple required fields")
        void shouldValidateMultipleRequiredFields() {
            // Given
            EventCustomField companyField = new EventCustomField();
            companyField.setId(2L);
            companyField.setFieldName("company");
            companyField.setFieldLabel("Company Name");
            companyField.setRequired(true);

            List<EventCustomField> requiredFields = List.of(testCustomField, companyField);

            Map<String, String> incompleteCustomFields = new HashMap<>();
            incompleteCustomFields.put("dietary_requirements", "Vegetarian");
            // Missing company field

            AttendeeInfo incompleteInfo = new AttendeeInfo(
                    "John", "Doe", "john@example.com", 1L, incompleteCustomFields
            );

            when(eventCustomFieldRepository.findRequiredFieldsByEventAndTicketType(1L, 1L))
                    .thenReturn(requiredFields);

            // When & Then
            ValidationException exception = assertThrows(ValidationException.class,
                    () -> attendeeService.createAttendee(incompleteInfo, testOrder, testTicket));

            assertEquals("Required field missing: Company Name", exception.getMessage());
        }

        @Test
        @DisplayName("Should pass validation when all required fields are provided")
        void shouldPassValidationWhenAllRequiredFieldsAreProvided() {
            // Given
            EventCustomField companyField = new EventCustomField();
            companyField.setFieldName("company");
            companyField.setFieldLabel("Company Name");
            companyField.setRequired(true);

            List<EventCustomField> requiredFields = List.of(testCustomField, companyField);

            Map<String, String> completeCustomFields = new HashMap<>();
            completeCustomFields.put("dietary_requirements", "Vegetarian");
            completeCustomFields.put("company", "Tech Corp");

            AttendeeInfo completeInfo = new AttendeeInfo(
                    "John", "Doe", "john@example.com", 1L, completeCustomFields
            );

            when(eventCustomFieldRepository.findRequiredFieldsByEventAndTicketType(1L, 1L))
                    .thenReturn(requiredFields);
            when(attendeeRepository.save(any(Attendee.class))).thenReturn(testAttendee);

            // When & Then
            assertDoesNotThrow(() -> attendeeService.createAttendee(completeInfo, testOrder, testTicket));
            verify(attendeeRepository).save(any(Attendee.class));
        }

        @Test
        @DisplayName("Should handle optional fields correctly")
        void shouldHandleOptionalFieldsCorrectly() {
            // Given
            EventCustomField optionalField = new EventCustomField();
            optionalField.setFieldName("phone");
            optionalField.setFieldLabel("Phone Number");
            optionalField.setRequired(false);

            Map<String, String> customFieldsWithOptional = new HashMap<>();
            customFieldsWithOptional.put("dietary_requirements", "Vegetarian");
            customFieldsWithOptional.put("phone", "+1234567890");

            AttendeeInfo infoWithOptional = new AttendeeInfo(
                    "John", "Doe", "john@example.com", 1L, customFieldsWithOptional
            );

            when(eventCustomFieldRepository.findRequiredFieldsByEventAndTicketType(1L, 1L))
                    .thenReturn(List.of(testCustomField)); // Only dietary_requirements is required
            when(attendeeRepository.save(any(Attendee.class))).thenReturn(testAttendee);

            // When & Then
            assertDoesNotThrow(() -> attendeeService.createAttendee(infoWithOptional, testOrder, testTicket));
            verify(attendeeRepository).save(any(Attendee.class));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle attendee with null ticket gracefully")
        void shouldHandleAttendeeWithNullTicketGracefully() {
            // Given
            Attendee attendeeWithoutTicket = new Attendee();
            attendeeWithoutTicket.setId(2L);
            attendeeWithoutTicket.setFirstName("Jane");
            attendeeWithoutTicket.setLastName("Smith");
            attendeeWithoutTicket.setEmail("jane@example.com");
            attendeeWithoutTicket.setTicket(null);
            attendeeWithoutTicket.setCheckInStatus(CheckInStatus.NOT_CHECKED_IN);
            attendeeWithoutTicket.setCustomFields(new ArrayList<>());

            // When & Then - This should throw an exception when trying to map to response
            assertThrows(NullPointerException.class, () -> {
                when(attendeeRepository.findByEventId(1L, PageRequest.of(0, 10)))
                        .thenReturn(new PageImpl<>(List.of(attendeeWithoutTicket)));
                attendeeService.getEventAttendees(1L, PageRequest.of(0, 10));
            });
        }

        @Test
        @DisplayName("Should handle repository exceptions gracefully")
        void shouldHandleRepositoryExceptionsGracefully() {
            // Given
            when(attendeeRepository.findById(1L)).thenThrow(new RuntimeException("Database error"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> attendeeService.checkInAttendee(1L, "admin@example.com"));

            assertEquals("Database error", exception.getMessage());
        }

        @Test
        @DisplayName("Should handle custom field update with empty list")
        void shouldHandleCustomFieldUpdateWithEmptyList() {
            // Given
            testAttendee.setCustomFields(new ArrayList<>());

            Map<String, String> newCustomFields = new HashMap<>();
            newCustomFields.put("new_field", "new_value");

            AttendeeInfo updateInfo = new AttendeeInfo(
                    "John", "Doe", "john@example.com", 1L, newCustomFields
            );

            when(attendeeRepository.findById(1L)).thenReturn(Optional.of(testAttendee));
            when(attendeeRepository.save(any(Attendee.class))).thenReturn(testAttendee);

            // When
            AttendeeResponse result = attendeeService.updateAttendee(1L, updateInfo);

            // Then
            assertNotNull(result);
            verify(attendeeRepository).save(any(Attendee.class));
        }

        @Test
        @DisplayName("Should handle transfer with event exactly at boundary")
        void shouldHandleTransferWithEventExactlyAtBoundary() {
            // Given
            testEvent.setStartDate(LocalDateTime.now().plusDays(1).plusSeconds(1)); // Just over 1 day

            TransferTicketRequest transferRequest = new TransferTicketRequest(
                    "Jane", "Smith", "jane@example.com", "Transfer message"
            );

            when(attendeeRepository.findById(1L)).thenReturn(Optional.of(testAttendee));

            // Mock the save method to return an attendee with initialized custom fields
            when(attendeeRepository.save(any(Attendee.class))).thenAnswer(invocation -> {
                Attendee savedAttendee = invocation.getArgument(0);
                // Initialize custom fields list if null to avoid NPE
                if (savedAttendee.getCustomFields() == null) {
                    savedAttendee.setCustomFields(new ArrayList<>());
                }
                savedAttendee.setId(2L); // Set an ID as would happen in real save
                return savedAttendee;
            });

            // When & Then
            assertDoesNotThrow(() -> attendeeService.transferTicket(1L, transferRequest));
            verify(attendeeRepository).delete(testAttendee);
            verify(attendeeRepository).save(any(Attendee.class));
        }
    }

    // Helper methods
    private Attendee createAttendee(Long id, String firstName, String lastName,
            CheckInStatus status, String ticketTypeName) {
        TicketType ticketType = new TicketType();
        ticketType.setName(ticketTypeName);

        Ticket ticket = new Ticket();
        ticket.setTicketCode("TKT-" + id);
        ticket.setTicketType(ticketType);

        Attendee attendee = new Attendee();
        attendee.setId(id);
        attendee.setFirstName(firstName);
        attendee.setLastName(lastName);
        attendee.setEmail(firstName.toLowerCase() + "@example.com");
        attendee.setTicket(ticket);
        attendee.setCheckInStatus(status);
        attendee.setCustomFields(new ArrayList<>());

        return attendee;
    }

}
