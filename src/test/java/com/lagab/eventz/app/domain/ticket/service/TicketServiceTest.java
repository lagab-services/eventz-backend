package com.lagab.eventz.app.domain.ticket.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

import com.lagab.eventz.app.common.exception.BusinessException;
import com.lagab.eventz.app.common.exception.ResourceNotFoundException;
import com.lagab.eventz.app.domain.event.model.Event;
import com.lagab.eventz.app.domain.event.model.TicketType;
import com.lagab.eventz.app.domain.order.model.Order;
import com.lagab.eventz.app.domain.order.model.OrderItem;
import com.lagab.eventz.app.domain.order.model.OrderStatus;
import com.lagab.eventz.app.domain.ticket.entity.Attendee;
import com.lagab.eventz.app.domain.ticket.entity.CheckInStatus;
import com.lagab.eventz.app.domain.ticket.entity.Ticket;
import com.lagab.eventz.app.domain.ticket.entity.TicketStatus;
import com.lagab.eventz.app.domain.ticket.repository.TicketRepository;
import com.lagab.eventz.app.domain.user.model.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketService Tests")
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private AttendeeService attendeeService;

    @InjectMocks
    private TicketService ticketService;

    private Order testOrder;
    private TicketType testTicketType1;
    private TicketType testTicketType2;
    private Event testEvent;
    private Attendee testAttendee1;
    private Attendee testAttendee2;

    @BeforeEach
    void setUp() {
        // Setup test user
        User testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        // Setup test event
        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Test Event");
        testEvent.setStartDate(LocalDateTime.now().plusDays(30));

        // Setup test ticket types
        testTicketType1 = new TicketType();
        testTicketType1.setId(1L);
        testTicketType1.setName("Standard");
        testTicketType1.setPrice(BigDecimal.valueOf(50.00));
        testTicketType1.setEvent(testEvent);

        testTicketType2 = new TicketType();
        testTicketType2.setId(2L);
        testTicketType2.setName("VIP");
        testTicketType2.setPrice(BigDecimal.valueOf(100.00));
        testTicketType2.setEvent(testEvent);

        // Setup test order
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setOrderNumber("ORD-123456");
        testOrder.setUser(testUser);
        testOrder.setEvent(testEvent);
        testOrder.setStatus(OrderStatus.PAID);
        testOrder.setCreatedAt(LocalDateTime.now());

        // Setup test order items
        OrderItem testOrderItem1 = new OrderItem();
        testOrderItem1.setId(1L);
        testOrderItem1.setOrder(testOrder);
        testOrderItem1.setTicketType(testTicketType1);
        testOrderItem1.setQuantity(2);
        testOrderItem1.setUnitPrice(BigDecimal.valueOf(50.00));
        testOrderItem1.setTotalPrice(BigDecimal.valueOf(100.00));

        OrderItem testOrderItem2 = new OrderItem();
        testOrderItem2.setId(2L);
        testOrderItem2.setOrder(testOrder);
        testOrderItem2.setTicketType(testTicketType2);
        testOrderItem2.setQuantity(1);
        testOrderItem2.setUnitPrice(BigDecimal.valueOf(100.00));
        testOrderItem2.setTotalPrice(BigDecimal.valueOf(100.00));

        testOrder.setOrderItems(List.of(testOrderItem1, testOrderItem2));

        // Setup test attendees
        testAttendee1 = new Attendee();
        testAttendee1.setId(1L);
        testAttendee1.setFirstName("John");
        testAttendee1.setLastName("Doe");
        testAttendee1.setEmail("john@example.com");
        testAttendee1.setOrder(testOrder);
        testAttendee1.setEvent(testEvent);
        testAttendee1.setCheckInStatus(CheckInStatus.NOT_CHECKED_IN);

        testAttendee2 = new Attendee();
        testAttendee2.setId(2L);
        testAttendee2.setFirstName("Jane");
        testAttendee2.setLastName("Smith");
        testAttendee2.setEmail("jane@example.com");
        testAttendee2.setOrder(testOrder);
        testAttendee2.setEvent(testEvent);
        testAttendee2.setCheckInStatus(CheckInStatus.NOT_CHECKED_IN);

        // Setup test ticket
        Ticket testTicket = new Ticket();
        testTicket.setId(1L);
        testTicket.setTicketCode("TKT-123456789ABC");
        testTicket.setQrCode("qr-code-123");
        testTicket.setStatus(TicketStatus.VALID);
        testTicket.setCheckedIn(false);
        testTicket.setOrder(testOrder);
        testTicket.setTicketType(testTicketType1);
        testTicket.setEvent(testEvent);
        testTicket.setCreatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("Generate Tickets For Order Tests")
    class GenerateTicketsForOrderTests {

        @Test
        @DisplayName("Should generate tickets successfully with attendees")
        void shouldGenerateTicketsSuccessfullyWithAttendees() {
            // Given
            List<Attendee> unassignedAttendees = List.of(testAttendee1, testAttendee2);
            List<Ticket> savedTickets = createExpectedTickets();

            when(attendeeService.findUnassignedAttendees(testOrder.getId())).thenReturn(unassignedAttendees);
            when(ticketRepository.saveAll(any())).thenReturn(savedTickets);

            // When
            List<Ticket> result = ticketService.generateTicketsForOrder(testOrder);

            // Then
            assertNotNull(result);
            assertEquals(3, result.size()); // 2 Standard + 1 VIP

            // Verify ticket repository interaction
            ArgumentCaptor<List<Ticket>> ticketsCaptor = ArgumentCaptor.forClass(List.class);
            verify(ticketRepository).saveAll(ticketsCaptor.capture());

            List<Ticket> capturedTickets = ticketsCaptor.getValue();
            assertEquals(3, capturedTickets.size());

            // Verify ticket properties
            for (Ticket ticket : capturedTickets) {
                assertNotNull(ticket.getTicketCode());
                assertTrue(ticket.getTicketCode().startsWith("TKT-"));
                assertEquals(12, ticket.getTicketCode().length() - 4); // TKT- prefix + 12 chars
                assertNotNull(ticket.getQrCode());
                assertEquals(TicketStatus.VALID, ticket.getStatus());
                assertFalse(ticket.getCheckedIn());
                assertEquals(testOrder, ticket.getOrder());
                assertNotNull(ticket.getCreatedAt());
            }

            // Verify attendee assignment
            verify(attendeeService).findUnassignedAttendees(testOrder.getId());
        }

        @Test
        @DisplayName("Should generate tickets without attendees")
        void shouldGenerateTicketsWithoutAttendees() {
            // Given
            List<Attendee> emptyAttendeeList = new ArrayList<>();
            List<Ticket> savedTickets = createExpectedTickets();

            when(attendeeService.findUnassignedAttendees(testOrder.getId())).thenReturn(emptyAttendeeList);
            when(ticketRepository.saveAll(any())).thenReturn(savedTickets);

            // When
            List<Ticket> result = ticketService.generateTicketsForOrder(testOrder);

            // Then
            assertNotNull(result);
            assertEquals(3, result.size());

            // Verify no attendees are assigned
            ArgumentCaptor<List<Ticket>> ticketsCaptor = ArgumentCaptor.forClass(List.class);
            verify(ticketRepository).saveAll(ticketsCaptor.capture());

            List<Ticket> capturedTickets = ticketsCaptor.getValue();
            for (Ticket ticket : capturedTickets) {
                assertNull(ticket.getAttendee());
            }
        }

        @Test
        @DisplayName("Should generate tickets with partial attendee assignment")
        void shouldGenerateTicketsWithPartialAttendeeAssignment() {
            // Given - Only one attendee for 3 tickets
            List<Attendee> singleAttendee = List.of(testAttendee1);
            List<Ticket> savedTickets = createExpectedTickets();

            when(attendeeService.findUnassignedAttendees(testOrder.getId())).thenReturn(singleAttendee);
            when(ticketRepository.saveAll(any())).thenReturn(savedTickets);

            // When
            List<Ticket> result = ticketService.generateTicketsForOrder(testOrder);

            // Then
            assertNotNull(result);
            assertEquals(3, result.size());

            // Verify only first ticket has attendee assigned
            ArgumentCaptor<List<Ticket>> ticketsCaptor = ArgumentCaptor.forClass(List.class);
            verify(ticketRepository).saveAll(ticketsCaptor.capture());

            List<Ticket> capturedTickets = ticketsCaptor.getValue();

            // First ticket should have attendee
            assertEquals(testAttendee1, capturedTickets.getFirst().getAttendee());

            // Other tickets should not have attendees
            assertNull(capturedTickets.get(1).getAttendee());
            assertNull(capturedTickets.get(2).getAttendee());
        }

        @Test
        @DisplayName("Should generate correct number of tickets per order item")
        void shouldGenerateCorrectNumberOfTicketsPerOrderItem() {
            // Given
            when(attendeeService.findUnassignedAttendees(testOrder.getId())).thenReturn(new ArrayList<>());
            when(ticketRepository.saveAll(any())).thenReturn(createExpectedTickets());

            // When
            ticketService.generateTicketsForOrder(testOrder);

            // Then
            ArgumentCaptor<List<Ticket>> ticketsCaptor = ArgumentCaptor.forClass(List.class);
            verify(ticketRepository).saveAll(ticketsCaptor.capture());

            List<Ticket> capturedTickets = ticketsCaptor.getValue();

            // Count tickets by type
            long standardTickets = capturedTickets.stream()
                                                  .filter(t -> t.getTicketType().equals(testTicketType1))
                                                  .count();
            long vipTickets = capturedTickets.stream()
                                             .filter(t -> t.getTicketType().equals(testTicketType2))
                                             .count();

            assertEquals(2, standardTickets); // testOrderItem1.quantity = 2
            assertEquals(1, vipTickets);      // testOrderItem2.quantity = 1
        }
    }

    @Nested
    @DisplayName("Check In Ticket Tests")
    class CheckInTicketTests {

        @Test
        @DisplayName("Should check in ticket successfully")
        void shouldCheckInTicketSuccessfully() {
            // Given
            String ticketCode = "TKT-123456789ABC";
            Ticket validTicket = createValidTicket();

            when(ticketRepository.findByTicketCode(ticketCode)).thenReturn(Optional.of(validTicket));
            when(ticketRepository.save(any(Ticket.class))).thenReturn(validTicket);

            // When
            Ticket result = ticketService.checkInTicket(ticketCode);

            // Then
            assertNotNull(result);
            assertTrue(result.getCheckedIn());
            assertNotNull(result.getCheckInTime());

            verify(ticketRepository).findByTicketCode(ticketCode);
            verify(ticketRepository).save(validTicket);
        }

        @Test
        @DisplayName("Should throw exception when ticket not found")
        void shouldThrowExceptionWhenTicketNotFound() {
            // Given
            String ticketCode = "INVALID-CODE";
            when(ticketRepository.findByTicketCode(ticketCode)).thenReturn(Optional.empty());

            // When & Then
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> ticketService.checkInTicket(ticketCode));

            assertEquals("Ticket not found", exception.getMessage());
            verify(ticketRepository).findByTicketCode(ticketCode);
            verify(ticketRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when ticket is not valid")
        void shouldThrowExceptionWhenTicketIsNotValid() {
            // Given
            String ticketCode = "TKT-123456789ABC";
            Ticket cancelledTicket = createValidTicket();
            cancelledTicket.setStatus(TicketStatus.CANCELLED);

            when(ticketRepository.findByTicketCode(ticketCode)).thenReturn(Optional.of(cancelledTicket));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> ticketService.checkInTicket(ticketCode));

            assertEquals("This ticket is not valid", exception.getMessage());
            verify(ticketRepository).findByTicketCode(ticketCode);
            verify(ticketRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when ticket already checked in")
        void shouldThrowExceptionWhenTicketAlreadyCheckedIn() {
            // Given
            String ticketCode = "TKT-123456789ABC";
            Ticket checkedInTicket = createValidTicket();
            checkedInTicket.setCheckedIn(true);
            checkedInTicket.setCheckInTime(LocalDateTime.now().minusHours(1));

            when(ticketRepository.findByTicketCode(ticketCode)).thenReturn(Optional.of(checkedInTicket));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> ticketService.checkInTicket(ticketCode));

            assertEquals("This ticket has already been scanned", exception.getMessage());
            verify(ticketRepository).findByTicketCode(ticketCode);
            verify(ticketRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle ticket with null checked in status")
        void shouldHandleTicketWithNullCheckedInStatus() {
            // Given
            String ticketCode = "TKT-123456789ABC";
            Ticket ticketWithNullStatus = createValidTicket();
            ticketWithNullStatus.setCheckedIn(null);

            when(ticketRepository.findByTicketCode(ticketCode)).thenReturn(Optional.of(ticketWithNullStatus));
            when(ticketRepository.save(any(Ticket.class))).thenReturn(ticketWithNullStatus);

            // When
            Ticket result = ticketService.checkInTicket(ticketCode);

            // Then
            assertNotNull(result);
            assertTrue(result.getCheckedIn());
            assertNotNull(result.getCheckInTime());

            verify(ticketRepository).save(ticketWithNullStatus);
        }

        @Test
        @DisplayName("Should handle different ticket statuses")
        void shouldHandleDifferentTicketStatuses() {
            // Test each invalid status
            TicketStatus[] invalidStatuses = {
                    TicketStatus.CANCELLED,
                    TicketStatus.REFUNDED,
                    TicketStatus.TRANSFERRED
            };

            for (TicketStatus status : invalidStatuses) {
                // Given
                String ticketCode = "TKT-" + status.name();
                Ticket invalidTicket = createValidTicket();
                invalidTicket.setStatus(status);

                when(ticketRepository.findByTicketCode(ticketCode)).thenReturn(Optional.of(invalidTicket));

                // When & Then
                BusinessException exception = assertThrows(BusinessException.class,
                        () -> ticketService.checkInTicket(ticketCode));

                assertEquals("This ticket is not valid", exception.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("Ticket Code Generation Tests")
    class TicketCodeGenerationTests {

        @Test
        @DisplayName("Should generate unique ticket codes")
        void shouldGenerateUniqueTicketCodes() {
            // Given
            when(attendeeService.findUnassignedAttendees(testOrder.getId())).thenReturn(new ArrayList<>());
            when(ticketRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            List<Ticket> result = ticketService.generateTicketsForOrder(testOrder);

            // Then
            List<String> ticketCodes = result.stream()
                                             .map(Ticket::getTicketCode)
                                             .toList();

            // Verify all codes are unique
            assertEquals(ticketCodes.size(), ticketCodes.stream().distinct().count());

            // Verify format
            for (String code : ticketCodes) {
                assertTrue(code.startsWith("TKT-"));
                assertEquals(16, code.length()); // TKT- (4) + 12 chars
                assertTrue(code.substring(4).matches("[A-Z0-9]+"));
            }
        }

        @Test
        @DisplayName("Should generate unique QR codes")
        void shouldGenerateUniqueQRCodes() {
            // Given
            when(attendeeService.findUnassignedAttendees(testOrder.getId())).thenReturn(new ArrayList<>());
            when(ticketRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            List<Ticket> result = ticketService.generateTicketsForOrder(testOrder);

            // Then
            List<String> qrCodes = result.stream()
                                         .map(Ticket::getQrCode)
                                         .toList();

            // Verify all QR codes are unique
            assertEquals(qrCodes.size(), qrCodes.stream().distinct().count());

            // Verify format (UUID format)
            for (String qrCode : qrCodes) {
                assertNotNull(qrCode);
                assertFalse(qrCode.isEmpty());
                // UUID format: 8-4-4-4-12 characters separated by hyphens
                assertTrue(qrCode.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"));
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle order with zero quantity items")
        void shouldHandleOrderWithZeroQuantityItems() {
            // Given
            OrderItem zeroQuantityItem = new OrderItem();
            zeroQuantityItem.setId(3L);
            zeroQuantityItem.setOrder(testOrder);
            zeroQuantityItem.setTicketType(testTicketType1);
            zeroQuantityItem.setQuantity(0);

            Order orderWithZeroQuantity = new Order();
            orderWithZeroQuantity.setId(3L);
            orderWithZeroQuantity.setOrderItems(List.of(zeroQuantityItem));

            when(attendeeService.findUnassignedAttendees(orderWithZeroQuantity.getId())).thenReturn(new ArrayList<>());
            when(ticketRepository.saveAll(any())).thenReturn(new ArrayList<>());

            // When
            List<Ticket> result = ticketService.generateTicketsForOrder(orderWithZeroQuantity);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should handle large quantity orders")
        void shouldHandleLargeQuantityOrders() {
            // Given
            OrderItem largeQuantityItem = new OrderItem();
            largeQuantityItem.setId(4L);
            largeQuantityItem.setOrder(testOrder);
            largeQuantityItem.setTicketType(testTicketType1);
            largeQuantityItem.setQuantity(100);

            Order largeOrder = new Order();
            largeOrder.setId(4L);
            largeOrder.setOrderItems(List.of(largeQuantityItem));

            when(attendeeService.findUnassignedAttendees(largeOrder.getId())).thenReturn(new ArrayList<>());
            when(ticketRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            List<Ticket> result = ticketService.generateTicketsForOrder(largeOrder);

            // Then
            assertNotNull(result);
            assertEquals(100, result.size());

            // Verify all tickets have correct properties
            for (Ticket ticket : result) {
                assertEquals(testTicketType1, ticket.getTicketType());
                assertEquals(largeOrder, ticket.getOrder());
                assertEquals(TicketStatus.VALID, ticket.getStatus());
            }
        }

        @Test
        @DisplayName("Should handle attendee assignment when more attendees than tickets")
        void shouldHandleMoreAttendeesThanTickets() {
            // Given - Create order with 1 ticket but 3 attendees
            OrderItem singleTicketItem = new OrderItem();
            singleTicketItem.setId(5L);
            singleTicketItem.setOrder(testOrder);
            singleTicketItem.setTicketType(testTicketType1);
            singleTicketItem.setQuantity(1);

            Order singleTicketOrder = new Order();
            singleTicketOrder.setId(5L);
            singleTicketOrder.setOrderItems(List.of(singleTicketItem));

            Attendee testAttendee3 = new Attendee();
            testAttendee3.setId(3L);
            testAttendee3.setFirstName("Bob");
            testAttendee3.setLastName("Johnson");

            List<Attendee> moreAttendees = List.of(testAttendee1, testAttendee2, testAttendee3);

            when(attendeeService.findUnassignedAttendees(singleTicketOrder.getId())).thenReturn(moreAttendees);
            when(ticketRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            List<Ticket> result = ticketService.generateTicketsForOrder(singleTicketOrder);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());

            // Only first attendee should be assigned
            Ticket ticket = result.getFirst();
            assertEquals(testAttendee1, ticket.getAttendee());
        }

        @Test
        @DisplayName("Should handle repository save failure gracefully")
        void shouldHandleRepositorySaveFailure() {
            // Given
            when(attendeeService.findUnassignedAttendees(testOrder.getId())).thenReturn(new ArrayList<>());
            when(ticketRepository.saveAll(any())).thenThrow(new RuntimeException("Database error"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> ticketService.generateTicketsForOrder(testOrder));

            assertEquals("Database error", exception.getMessage());
        }

        @Test
        @DisplayName("Should preserve ticket creation timestamp")
        void shouldPreserveTicketCreationTimestamp() {
            // Given
            LocalDateTime beforeGeneration = LocalDateTime.now();

            when(attendeeService.findUnassignedAttendees(testOrder.getId())).thenReturn(new ArrayList<>());
            when(ticketRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            List<Ticket> result = ticketService.generateTicketsForOrder(testOrder);

            // Then
            LocalDateTime afterGeneration = LocalDateTime.now();

            for (Ticket ticket : result) {
                assertNotNull(ticket.getCreatedAt());
                assertTrue(ticket.getCreatedAt().isAfter(beforeGeneration.minusSeconds(1)));
                assertTrue(ticket.getCreatedAt().isBefore(afterGeneration.plusSeconds(1)));
            }
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle complete ticket generation workflow")
        void shouldHandleCompleteTicketGenerationWorkflow() {
            // Given - Complex order with multiple ticket types and attendees
            List<Attendee> attendees = List.of(testAttendee1, testAttendee2);

            when(attendeeService.findUnassignedAttendees(testOrder.getId())).thenReturn(attendees);
            when(ticketRepository.saveAll(any())).thenAnswer(invocation -> {
                List<Ticket> tickets = invocation.getArgument(0);
                // Simulate setting IDs as would happen in real save
                for (int i = 0; i < tickets.size(); i++) {
                    tickets.get(i).setId((long) (i + 1));
                }
                return tickets;
            });

            // When
            List<Ticket> result = ticketService.generateTicketsForOrder(testOrder);

            // Then
            assertNotNull(result);
            assertEquals(3, result.size());

            // Verify attendee assignments
            assertTrue(result.stream().anyMatch(t -> t.getAttendee() == testAttendee1));
            assertTrue(result.stream().anyMatch(t -> t.getAttendee() == testAttendee2));

            // Verify ticket types distribution
            long standardTickets = result.stream().filter(t -> t.getTicketType() == testTicketType1).count();
            long vipTickets = result.stream().filter(t -> t.getTicketType() == testTicketType2).count();
            assertEquals(2, standardTickets);
            assertEquals(1, vipTickets);

            // Verify all tickets have required properties
            for (Ticket ticket : result) {
                assertNotNull(ticket.getId());
                assertNotNull(ticket.getTicketCode());
                assertNotNull(ticket.getQrCode());
                assertEquals(TicketStatus.VALID, ticket.getStatus());
                assertFalse(ticket.getCheckedIn());
                assertEquals(testOrder, ticket.getOrder());
                assertNotNull(ticket.getTicketType());
                assertNotNull(ticket.getCreatedAt());
            }

            // Verify service interactions
            verify(attendeeService).findUnassignedAttendees(testOrder.getId());
            verify(ticketRepository).saveAll(any());
        }
    }

    // Helper methods
    private List<Ticket> createExpectedTickets() {
        List<Ticket> tickets = new ArrayList<>();

        // 2 Standard tickets
        for (int i = 0; i < 2; i++) {
            Ticket ticket = new Ticket();
            ticket.setId((long) (i + 1));
            ticket.setTicketCode("TKT-STANDARD" + i);
            ticket.setQrCode("qr-standard-" + i);
            ticket.setStatus(TicketStatus.VALID);
            ticket.setCheckedIn(false);
            ticket.setOrder(testOrder);
            ticket.setTicketType(testTicketType1);
            ticket.setEvent(testEvent);
            ticket.setCreatedAt(LocalDateTime.now());
            tickets.add(ticket);
        }

        // 1 VIP ticket
        Ticket vipTicket = new Ticket();
        vipTicket.setId(3L);
        vipTicket.setTicketCode("TKT-VIP1");
        vipTicket.setQrCode("qr-vip-1");
        vipTicket.setStatus(TicketStatus.VALID);
        vipTicket.setCheckedIn(false);
        vipTicket.setOrder(testOrder);
        vipTicket.setTicketType(testTicketType2);
        vipTicket.setEvent(testEvent);
        vipTicket.setCreatedAt(LocalDateTime.now());
        tickets.add(vipTicket);

        return tickets;
    }

    private Ticket createValidTicket() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setTicketCode("TKT-123456789ABC");
        ticket.setQrCode("qr-code-123");
        ticket.setStatus(TicketStatus.VALID);
        ticket.setCheckedIn(false);
        ticket.setOrder(testOrder);
        ticket.setTicketType(testTicketType1);
        ticket.setEvent(testEvent);
        ticket.setCreatedAt(LocalDateTime.now());
        return ticket;
    }
}
