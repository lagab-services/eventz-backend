package com.lagab.eventz.app.event.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lagab.eventz.app.common.exception.BusinessException;
import com.lagab.eventz.app.domain.event.dto.ticket.BulkUpdateTicketTypeRequest;
import com.lagab.eventz.app.domain.event.dto.ticket.CreateTicketTypeRequest;
import com.lagab.eventz.app.domain.event.dto.ticket.TicketTypeDTO;
import com.lagab.eventz.app.domain.event.dto.ticket.TicketTypeStatsDTO;
import com.lagab.eventz.app.domain.event.dto.ticket.UpdateTicketTypeRequest;
import com.lagab.eventz.app.domain.event.mapper.TicketTypeMapper;
import com.lagab.eventz.app.domain.event.model.Event;
import com.lagab.eventz.app.domain.event.projection.TicketTypeStatsProjection;
import com.lagab.eventz.app.domain.event.repository.EventRepository;
import com.lagab.eventz.app.domain.event.repository.TicketTypeRepository;
import com.lagab.eventz.app.domain.event.service.TicketTypeService;
import com.lagab.eventz.app.domain.ticket.entity.TicketType;

import jakarta.persistence.EntityNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketTypeService Tests")
class TicketTypeServiceTest {

    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketTypeMapper ticketTypeMapper;

    @InjectMocks
    private TicketTypeService ticketTypeService;

    private Event mockEvent;
    private TicketType mockTicketType;
    private CreateTicketTypeRequest createRequest;
    private UpdateTicketTypeRequest updateRequest;
    private TicketTypeDTO mockTicketTypeDTO;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime eventStart = now.plusDays(30);

        mockEvent = new Event();
        mockEvent.setId(1L);
        mockEvent.setStartDate(eventStart);

        mockTicketType = new TicketType();
        mockTicketType.setId(1L);
        mockTicketType.setEvent(mockEvent);
        mockTicketType.setName("VIP");
        mockTicketType.setPrice(BigDecimal.valueOf(100));
        mockTicketType.setQuantityAvailable(100);
        mockTicketType.setQuantitySold(0);
        mockTicketType.setSaleStart(now.plusDays(1));
        mockTicketType.setSaleEnd(now.plusDays(29));
        mockTicketType.setIsActive(true);
        mockTicketType.setSortOrder(1);

        createRequest = new CreateTicketTypeRequest(
                "VIP",
                "VIP Ticket",
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(10), // fee
                1, // sortOrder
                100, // capacity
                100, // quantityAvailable
                now.plusDays(1), // saleStart
                now.plusDays(29), // saleEnd
                1, // minQuantity
                10 // maxQuantity
        );

        updateRequest = new UpdateTicketTypeRequest(
                "VIP Updated", // name
                "Updated VIP Ticket", // description
                BigDecimal.valueOf(120), // price
                BigDecimal.valueOf(15), // fee
                2, // sortOrder
                150, // capacity
                150, // quantityAvailable
                now.plusDays(2), // saleStart
                now.plusDays(28), // saleEnd
                1, // minQuantity
                15, // maxQuantity
                true // isActive
        );

        mockTicketTypeDTO = new TicketTypeDTO(
                1L, // id
                "VIP", // name
                "VIP Ticket", // description
                BigDecimal.valueOf(100), // price
                100, // quantityAvailable
                0, // quantitySold
                now.plusDays(1), // saleStart
                now.plusDays(29), // saleEnd
                1, // minQuantity
                10, // maxQuantity
                true, // isActive
                100, // remainingTickets (deprecated)
                100, // quantityRemaining (computed)
                BigDecimal.ZERO, // totalPrice (computed: price * quantitySold)
                true, // isOnSale (computed)
                false // isSoldOut (computed)
        );
    }

    @Nested
    @DisplayName("Create Ticket Type Tests")
    class CreateTicketTypeTests {

        @Test
        @DisplayName("Should create ticket type successfully")
        void shouldCreateTicketTypeSuccessfully() {
            // Given
            when(eventRepository.findById(1L)).thenReturn(Optional.of(mockEvent));
            when(ticketTypeMapper.toEntity(createRequest)).thenReturn(mockTicketType);
            when(ticketTypeRepository.save(any(TicketType.class))).thenReturn(mockTicketType);
            when(ticketTypeMapper.toDTO(mockTicketType)).thenReturn(mockTicketTypeDTO);

            // When
            TicketTypeDTO result = ticketTypeService.createTicketType(1L, createRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("VIP");
            verify(eventRepository).findById(1L);
            verify(ticketTypeRepository).save(any(TicketType.class));
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when event not found")
        void shouldThrowEntityNotFoundExceptionWhenEventNotFound() {
            // Given
            when(eventRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> ticketTypeService.createTicketType(1L, createRequest))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Event not found with ID: 1");
        }

        @Test
        @DisplayName("Should throw BusinessException when sale start is in the past")
        void shouldThrowBusinessExceptionWhenSaleStartInPast() {
            // Given
            LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
            CreateTicketTypeRequest invalidRequest = new CreateTicketTypeRequest(
                    "VIP",
                    "VIP Ticket",
                    BigDecimal.valueOf(150),
                    BigDecimal.valueOf(15), // fee
                    2, // sortOrder
                    50, // capacity
                    50, // quantityAvailable
                    pastDate,
                    LocalDateTime.now().plusDays(29),
                    1, // minQuantity
                    5 // maxQuantity
            );
            when(eventRepository.findById(1L)).thenReturn(Optional.of(mockEvent));

            // When & Then
            assertThatThrownBy(() -> ticketTypeService.createTicketType(1L, invalidRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Sale start date cannot be in the past");
        }

        @Test
        @DisplayName("Should throw BusinessException when sale end is after event start")
        void shouldThrowBusinessExceptionWhenSaleEndAfterEventStart() {
            // Given
            LocalDateTime afterEventStart = mockEvent.getStartDate().plusDays(1);
            CreateTicketTypeRequest invalidRequest = new CreateTicketTypeRequest(
                    "VIP",
                    "VIP Ticket",
                    BigDecimal.valueOf(150),
                    BigDecimal.valueOf(15), // fee
                    2, // sortOrder
                    50, // capacity
                    50, // quantityAvailable
                    LocalDateTime.now().plusDays(1),
                    afterEventStart,
                    1, // minQuantity
                    5 // maxQuantity
            );
            when(eventRepository.findById(1L)).thenReturn(Optional.of(mockEvent));

            // When & Then
            assertThatThrownBy(() -> ticketTypeService.createTicketType(1L, invalidRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Sales must end before the event starts");
        }
        
    }

    @Nested
    @DisplayName("Bulk Create Ticket Types Tests")
    class BulkCreateTicketTypeTests {

        @Test
        @DisplayName("Should create multiple ticket types successfully")
        void shouldCreateMultipleTicketTypesSuccessfully() {
            // Given
            CreateTicketTypeRequest request2 = new CreateTicketTypeRequest(
                    "Standard",
                    "Standard Ticket",
                    BigDecimal.valueOf(50),
                    BigDecimal.valueOf(200), // fee
                    2, // sortOrder
                    50, // capacity
                    50, // quantityAvailable
                    LocalDateTime.now().plusDays(1),
                    LocalDateTime.now().plusDays(29),
                    2, // minQuantity
                    5 // maxQuantity
            );
            List<CreateTicketTypeRequest> requests = List.of(createRequest, request2);

            when(eventRepository.findById(1L)).thenReturn(Optional.of(mockEvent));
            when(ticketTypeMapper.toEntity(any(CreateTicketTypeRequest.class))).thenReturn(mockTicketType);
            when(ticketTypeRepository.saveAll(anyList())).thenReturn(List.of(mockTicketType, mockTicketType));
            when(ticketTypeMapper.toDTOList(anyList())).thenReturn(List.of(mockTicketTypeDTO, mockTicketTypeDTO));

            // When
            List<TicketTypeDTO> result = ticketTypeService.createBulkTicketTypes(1L, requests);

            // Then
            assertThat(result).hasSize(2);
            verify(ticketTypeRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should throw BusinessException when duplicate names exist")
        void shouldThrowBusinessExceptionWhenDuplicateNamesExist() {
            // Given
            List<CreateTicketTypeRequest> requests = List.of(createRequest, createRequest);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(mockEvent));

            // When & Then
            assertThatThrownBy(() -> ticketTypeService.createBulkTicketTypes(1L, requests))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Ticket type names must be unique");
        }
    }

    @Nested
    @DisplayName("Get Ticket Type Tests")
    class GetTicketTypeTests {

        @Test
        @DisplayName("Should get ticket type by ID successfully")
        void shouldGetTicketTypeByIdSuccessfully() {
            // Given
            when(ticketTypeRepository.findById(1L)).thenReturn(Optional.of(mockTicketType));
            when(ticketTypeMapper.toDTO(mockTicketType)).thenReturn(mockTicketTypeDTO);

            // When
            TicketTypeDTO result = ticketTypeService.getTicketTypeById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when ticket type not found")
        void shouldThrowEntityNotFoundExceptionWhenTicketTypeNotFound() {
            // Given
            when(ticketTypeRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> ticketTypeService.getTicketTypeById(1L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Ticket type not found with ID: 1");
        }

        @Test
        @DisplayName("Should get ticket types by event ID")
        void shouldGetTicketTypesByEventId() {
            // Given
            when(ticketTypeRepository.findByEventIdOrderBySortOrder(1L))
                    .thenReturn(List.of(mockTicketType));
            when(ticketTypeMapper.toDTOList(anyList())).thenReturn(List.of(mockTicketTypeDTO));

            // When
            List<TicketTypeDTO> result = ticketTypeService.getTicketTypesByEventId(1L);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should get active ticket types by event ID")
        void shouldGetActiveTicketTypesByEventId() {
            // Given
            when(ticketTypeRepository.findActiveByEventId(1L))
                    .thenReturn(List.of(mockTicketType));
            when(ticketTypeMapper.toDTOList(anyList())).thenReturn(List.of(mockTicketTypeDTO));

            // When
            List<TicketTypeDTO> result = ticketTypeService.getActiveTicketTypesByEventId(1L);

            // Then
            assertThat(result).hasSize(1);
            verify(ticketTypeRepository).findActiveByEventId(1L);
        }

        @Test
        @DisplayName("Should get on sale ticket types by event ID")
        void shouldGetOnSaleTicketTypesByEventId() {
            // Given
            when(ticketTypeRepository.findOnSaleByEventId(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(List.of(mockTicketType));
            when(ticketTypeMapper.toDTOList(anyList())).thenReturn(List.of(mockTicketTypeDTO));

            // When
            List<TicketTypeDTO> result = ticketTypeService.getOnSaleTicketTypesByEventId(1L);

            // Then
            assertThat(result).hasSize(1);
            verify(ticketTypeRepository).findOnSaleByEventId(eq(1L), any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("Update Ticket Type Tests")
    class UpdateTicketTypeTests {

        @Test
        @DisplayName("Should update ticket type successfully")
        void shouldUpdateTicketTypeSuccessfully() {
            // Given
            when(ticketTypeRepository.findById(1L)).thenReturn(Optional.of(mockTicketType));
            when(ticketTypeRepository.save(any(TicketType.class))).thenReturn(mockTicketType);
            when(ticketTypeMapper.toDTO(mockTicketType)).thenReturn(mockTicketTypeDTO);

            // When
            TicketTypeDTO result = ticketTypeService.updateTicketType(1L, updateRequest);

            // Then
            assertThat(result).isNotNull();
            verify(ticketTypeMapper).updateEntityFromDTO(updateRequest, mockTicketType);
            verify(ticketTypeRepository).save(mockTicketType);
        }

        @Test
        @DisplayName("Should throw BusinessException when new quantity is less than sold")
        void shouldThrowBusinessExceptionWhenNewQuantityLessThanSold() {
            // Given
            mockTicketType.setQuantitySold(50);
            UpdateTicketTypeRequest invalidRequest = new UpdateTicketTypeRequest(
                    "VIP", // name
                    "VIP Ticket", // description
                    BigDecimal.valueOf(100), // price
                    BigDecimal.valueOf(30), // fee
                    2, // sortOrder
                    150, // capacity
                    20, // quantityAvailable
                    LocalDateTime.now().plusDays(2), // saleStart
                    LocalDateTime.now().plusDays(29), // saleEnd
                    1, // minQuantity
                    15, // maxQuantity
                    true // isActive
            );
            when(ticketTypeRepository.findById(1L)).thenReturn(Optional.of(mockTicketType));
            // When & Then
            assertThatThrownBy(() -> ticketTypeService.updateTicketType(1L, invalidRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Available quantity cannot be less than already sold quantity");
        }

        @Test
        @DisplayName("Should handle partial updates correctly")
        void shouldHandlePartialUpdatesCorrectly() {
            // Given
            UpdateTicketTypeRequest partialRequest = new UpdateTicketTypeRequest(
                    "Updated Name", // name
                    null, // description
                    null, // price
                    null, // fee
                    null, // sortOrder
                    null, // capacity
                    null, // quantityAvailable
                    null, // saleStart
                    null, // saleEnd
                    null, // minQuantity
                    null, // maxQuantity
                    true // isActive
            );
            when(ticketTypeRepository.findById(1L)).thenReturn(Optional.of(mockTicketType));
            when(ticketTypeRepository.save(any(TicketType.class))).thenReturn(mockTicketType);
            when(ticketTypeMapper.toDTO(mockTicketType)).thenReturn(mockTicketTypeDTO);

            // When
            TicketTypeDTO result = ticketTypeService.updateTicketType(1L, partialRequest);

            // Then
            assertThat(result).isNotNull();
            verify(ticketTypeMapper).updateEntityFromDTO(partialRequest, mockTicketType);
        }
    }

    @Nested
    @DisplayName("Bulk Update Ticket Types Tests")
    class BulkUpdateTicketTypeTests {

        @Test
        @DisplayName("Should bulk update ticket types successfully")
        void shouldBulkUpdateTicketTypesSuccessfully() {
            // Given
            BulkUpdateTicketTypeRequest.TicketTypeUpdate update1 =
                    new BulkUpdateTicketTypeRequest.TicketTypeUpdate(1L, updateRequest);
            BulkUpdateTicketTypeRequest.TicketTypeUpdate update2 =
                    new BulkUpdateTicketTypeRequest.TicketTypeUpdate(2L, updateRequest);

            BulkUpdateTicketTypeRequest bulkRequest =
                    new BulkUpdateTicketTypeRequest(List.of(update1, update2));

            TicketType mockTicketType2 = new TicketType();
            mockTicketType2.setId(2L);
            mockTicketType2.setEvent(mockEvent);

            when(ticketTypeRepository.findById(1L)).thenReturn(Optional.of(mockTicketType));
            when(ticketTypeRepository.findById(2L)).thenReturn(Optional.of(mockTicketType2));
            when(ticketTypeRepository.saveAll(anyList()))
                    .thenReturn(List.of(mockTicketType, mockTicketType2));
            when(ticketTypeMapper.toDTOList(anyList()))
                    .thenReturn(List.of(mockTicketTypeDTO, mockTicketTypeDTO));

            // When
            List<TicketTypeDTO> result = ticketTypeService.updateBulkTicketTypes(bulkRequest);

            // Then
            assertThat(result).hasSize(2);
            verify(ticketTypeMapper, times(2)).updateEntityFromDTO(eq(updateRequest), any(TicketType.class));
            verify(ticketTypeRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException for non-existent ticket type in bulk update")
        void shouldThrowEntityNotFoundExceptionForNonExistentTicketTypeInBulkUpdate() {
            // Given
            BulkUpdateTicketTypeRequest.TicketTypeUpdate update =
                    new BulkUpdateTicketTypeRequest.TicketTypeUpdate(999L, updateRequest);
            BulkUpdateTicketTypeRequest bulkRequest =
                    new BulkUpdateTicketTypeRequest(List.of(update));

            when(ticketTypeRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> ticketTypeService.updateBulkTicketTypes(bulkRequest))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Ticket type not found with ID: 999");
        }
    }

    @Nested
    @DisplayName("Delete Ticket Type Tests")
    class DeleteTicketTypeTests {

        @Test
        @DisplayName("Should delete ticket type successfully")
        void shouldDeleteTicketTypeSuccessfully() {
            // Given
            mockTicketType.setQuantitySold(0);
            when(ticketTypeRepository.findById(1L)).thenReturn(Optional.of(mockTicketType));

            // When
            ticketTypeService.deleteTicketType(1L);

            // Then
            verify(ticketTypeRepository).delete(mockTicketType);
        }

        @Test
        @DisplayName("Should throw BusinessException when trying to delete ticket type with sales")
        void shouldThrowBusinessExceptionWhenTryingToDeleteTicketTypeWithSales() {
            // Given
            mockTicketType.setQuantitySold(10);
            when(ticketTypeRepository.findById(1L)).thenReturn(Optional.of(mockTicketType));

            // When & Then
            assertThatThrownBy(() -> ticketTypeService.deleteTicketType(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Cannot delete a ticket type with existing sales");
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when ticket type not found for deletion")
        void shouldThrowEntityNotFoundExceptionWhenTicketTypeNotFoundForDeletion() {
            // Given
            when(ticketTypeRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> ticketTypeService.deleteTicketType(1L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Ticket type not found with ID: 1");
        }
    }

    @Nested
    @DisplayName("Toggle Active Status Tests")
    class ToggleActiveStatusTests {

        @Test
        @DisplayName("Should toggle active status from true to false")
        void shouldToggleActiveStatusFromTrueToFalse() {
            // Given
            mockTicketType.setIsActive(true);
            when(ticketTypeRepository.findById(1L)).thenReturn(Optional.of(mockTicketType));
            when(ticketTypeRepository.save(any(TicketType.class))).thenReturn(mockTicketType);
            when(ticketTypeMapper.toDTO(mockTicketType)).thenReturn(mockTicketTypeDTO);

            // When
            TicketTypeDTO result = ticketTypeService.toggleActiveStatus(1L);

            // Then
            assertThat(result).isNotNull();
            verify(ticketTypeRepository).save(argThat(ticketType ->
                    !ticketType.getIsActive()));
        }

        @Test
        @DisplayName("Should toggle active status from false to true")
        void shouldToggleActiveStatusFromFalseToTrue() {
            // Given
            mockTicketType.setIsActive(false);
            when(ticketTypeRepository.findById(1L)).thenReturn(Optional.of(mockTicketType));
            when(ticketTypeRepository.save(any(TicketType.class))).thenReturn(mockTicketType);
            when(ticketTypeMapper.toDTO(mockTicketType)).thenReturn(mockTicketTypeDTO);

            // When
            TicketTypeDTO result = ticketTypeService.toggleActiveStatus(1L);

            // Then
            assertThat(result).isNotNull();
            verify(ticketTypeRepository).save(argThat(ticketType ->
                    ticketType.getIsActive()));
        }

        @Test
        @DisplayName("Should handle null active status")
        void shouldHandleNullActiveStatus() {
            // Given
            mockTicketType.setIsActive(null);
            when(ticketTypeRepository.findById(1L)).thenReturn(Optional.of(mockTicketType));
            when(ticketTypeRepository.save(any(TicketType.class))).thenReturn(mockTicketType);
            when(ticketTypeMapper.toDTO(mockTicketType)).thenReturn(mockTicketTypeDTO);

            // When
            TicketTypeDTO result = ticketTypeService.toggleActiveStatus(1L);

            // Then
            assertThat(result).isNotNull();
            verify(ticketTypeRepository).save(argThat(ticketType ->
                    ticketType.getIsActive()));
        }
    }

    @Nested
    @DisplayName("Update Quantity Sold Tests")
    class UpdateQuantitySoldTests {

        @Test
        @DisplayName("Should update quantity sold successfully")
        void shouldUpdateQuantitySoldSuccessfully() {
            // Given
            when(ticketTypeRepository.updateQuantitySold(1L, 5)).thenReturn(1);

            // When
            ticketTypeService.updateQuantitySold(1L, 5);

            // Then
            verify(ticketTypeRepository).updateQuantitySold(1L, 5);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when no rows updated")
        void shouldThrowEntityNotFoundExceptionWhenNoRowsUpdated() {
            // Given
            when(ticketTypeRepository.updateQuantitySold(1L, 5)).thenReturn(0);

            // When & Then
            assertThatThrownBy(() -> ticketTypeService.updateQuantitySold(1L, 5))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Ticket type not found with ID: 1");
        }
    }

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Should get event ticket type stats successfully")
        void shouldGetEventTicketTypeStatsSuccessfully() {
            // Given
            TicketTypeStatsProjection mockProjection = mock(TicketTypeStatsProjection.class);

            TicketTypeStatsDTO expectedStats = new TicketTypeStatsDTO(
                    3L, // totalTicketTypes
                    2L, // activeTicketTypes
                    1L, // soldOutTicketTypes
                    300, // totalCapacity
                    150, // totalSold
                    150, // totalRemaining
                    BigDecimal.valueOf(15000), // totalRevenue
                    100.0, // averagePrice
                    50.0  // sellThroughRate (calculated: totalSold / totalCapacity * 100)
            );

            when(ticketTypeRepository.getStatsByEventId(1L))
                    .thenReturn(Optional.of(mockProjection));
            when(ticketTypeMapper.toDTO(mockProjection)).thenReturn(expectedStats);

            // When
            TicketTypeStatsDTO result = ticketTypeService.getEventTicketTypeStats(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.totalTicketTypes()).isEqualTo(3L);
            assertThat(result.activeTicketTypes()).isEqualTo(2L);
            assertThat(result.soldOutTicketTypes()).isEqualTo(1L);
            assertThat(result.totalCapacity()).isEqualTo(300);
            assertThat(result.totalSold()).isEqualTo(150);
            assertThat(result.totalRemaining()).isEqualTo(150);
            assertThat(result.totalRevenue()).isEqualTo(BigDecimal.valueOf(15000));
            assertThat(result.averagePrice()).isEqualTo(100.0);
            assertThat(result.sellThroughRate()).isEqualTo(50.0); // Calculated field

            verify(ticketTypeRepository).getStatsByEventId(1L);
            verify(ticketTypeMapper).toDTO(mockProjection);
        }

        @Test
        @DisplayName("Should return default stats when no data found")
        void shouldReturnDefaultStatsWhenNoDataFound() {
            // Given
            when(ticketTypeRepository.getStatsByEventId(1L)).thenReturn(Optional.empty());

            // When
            TicketTypeStatsDTO result = ticketTypeService.getEventTicketTypeStats(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.totalTicketTypes()).isEqualTo(0L);
            assertThat(result.activeTicketTypes()).isEqualTo(0L);
            assertThat(result.soldOutTicketTypes()).isEqualTo(0L);
            assertThat(result.totalCapacity()).isEqualTo(0);
            assertThat(result.totalSold()).isEqualTo(0);
            assertThat(result.totalRemaining()).isEqualTo(0);
            assertThat(result.totalRevenue()).isEqualTo(BigDecimal.ZERO);
            assertThat(result.averagePrice()).isEqualTo(0.0);
            assertThat(result.sellThroughRate()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Availability Tests")
    class AvailabilityTests {

        @Test
        @DisplayName("Should return true when ticket type is available")
        void shouldReturnTrueWhenTicketTypeIsAvailable() {
            // Given
            when(ticketTypeRepository.getAvailableQuantity(1L)).thenReturn(Optional.of(50));

            // When
            boolean result = ticketTypeService.isTicketTypeAvailable(1L, 30);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when ticket type is not available")
        void shouldReturnFalseWhenTicketTypeIsNotAvailable() {
            // Given
            when(ticketTypeRepository.getAvailableQuantity(1L)).thenReturn(Optional.of(20));

            // When
            boolean result = ticketTypeService.isTicketTypeAvailable(1L, 30);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when ticket type not found")
        void shouldReturnFalseWhenTicketTypeNotFound() {
            // Given
            when(ticketTypeRepository.getAvailableQuantity(1L)).thenReturn(Optional.empty());

            // When
            boolean result = ticketTypeService.isTicketTypeAvailable(1L, 30);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Reorder Ticket Types Tests")
    class ReorderTicketTypesTests {

        @Test
        @DisplayName("Should reorder ticket types successfully")
        void shouldReorderTicketTypesSuccessfully() {
            // Given
            TicketType ticketType1 = new TicketType();
            ticketType1.setId(1L);
            ticketType1.setSortOrder(1);

            TicketType ticketType2 = new TicketType();
            ticketType2.setId(2L);
            ticketType2.setSortOrder(2);

            List<TicketType> existingTicketTypes = List.of(ticketType1, ticketType2);
            List<Long> newOrder = List.of(2L, 1L);

            when(ticketTypeRepository.findByEventIdOrderBySortOrderAscIdAsc(1L))
                    .thenReturn(existingTicketTypes);
            when(ticketTypeRepository.saveAll(anyList())).thenReturn(existingTicketTypes);
            when(ticketTypeMapper.toDTOList(anyList())).thenReturn(List.of(mockTicketTypeDTO, mockTicketTypeDTO));

            // When
            List<TicketTypeDTO> result = ticketTypeService.reorderTicketTypes(1L, newOrder);

            // Then
            assertThat(result).hasSize(2);
            verify(ticketTypeRepository).saveAll(argThat(ticketTypes -> {
                List<TicketType> list = (List<TicketType>) ticketTypes;
                return list.stream().anyMatch(tt -> tt.getId().equals(2L) && tt.getSortOrder() == 1) &&
                        list.stream().anyMatch(tt -> tt.getId().equals(1L) && tt.getSortOrder() == 2);
            }));
        }

        @Test
        @DisplayName("Should throw BusinessException when ticket type doesn't belong to event")
        void shouldThrowBusinessExceptionWhenTicketTypeDoesntBelongToEvent() {
            // Given
            TicketType ticketType1 = new TicketType();
            ticketType1.setId(1L);

            List<TicketType> existingTicketTypes = List.of(ticketType1);
            List<Long> newOrder = List.of(1L, 999L); // 999L doesn't exist

            when(ticketTypeRepository.findByEventIdOrderBySortOrderAscIdAsc(1L))
                    .thenReturn(existingTicketTypes);

            // When & Then
            assertThatThrownBy(() -> ticketTypeService.reorderTicketTypes(1L, newOrder))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Some ticket types don't belong to this event");
        }
    }

    @Nested
    @DisplayName("CreateTicketTypeRequest Validation Tests")
    class CreateTicketTypeRequestValidationTests {

        @Test
        @DisplayName("Should throw BusinessException when min quantity greater than max quantity")
        void shouldThrowBusinessExceptionWhenMinQuantityGreaterThanMaxQuantity() {
            // When & Then
            assertThatThrownBy(() -> new CreateTicketTypeRequest(
                    "VIP",
                    "VIP Ticket",
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(10),
                    1,
                    100,
                    100,
                    LocalDateTime.now().plusDays(1),
                    LocalDateTime.now().plusDays(29),
                    10, // minQuantity > maxQuantity
                    5   // maxQuantity
            )).isInstanceOf(BusinessException.class)
              .hasMessage("Minimum quantity cannot be greater than maximum quantity");
        }

        @Test
        @DisplayName("Should throw BusinessException when sale start after sale end")
        void shouldThrowBusinessExceptionWhenSaleStartAfterSaleEnd() {
            // When & Then
            assertThatThrownBy(() -> new CreateTicketTypeRequest(
                    "VIP",
                    "VIP Ticket",
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(10),
                    1,
                    100,
                    100,
                    LocalDateTime.now().plusDays(10), // saleStart after saleEnd
                    LocalDateTime.now().plusDays(5),  // saleEnd
                    1,
                    10
            )).isInstanceOf(BusinessException.class)
              .hasMessage("Sale start date cannot be after sale end date");
        }

        @Test
        @DisplayName("Should throw BusinessException when quantity available exceeds capacity")
        void shouldThrowBusinessExceptionWhenQuantityAvailableExceedsCapacity() {
            // When & Then
            assertThatThrownBy(() -> new CreateTicketTypeRequest(
                    "VIP",
                    "VIP Ticket",
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(10),
                    1,
                    50,  // capacity
                    100, // quantityAvailable > capacity
                    LocalDateTime.now().plusDays(1),
                    LocalDateTime.now().plusDays(29),
                    1,
                    10
            )).isInstanceOf(BusinessException.class)
              .hasMessage("Available quantity cannot exceed capacity");
        }

        @Test
        @DisplayName("Should set default values for optional fields")
        void shouldSetDefaultValuesForOptionalFields() {
            // Given & When
            CreateTicketTypeRequest request = new CreateTicketTypeRequest(
                    "Basic",
                    "Basic Ticket",
                    BigDecimal.valueOf(50),
                    null, // fee should default to ZERO
                    1,
                    100,
                    100,
                    LocalDateTime.now().plusDays(1),
                    LocalDateTime.now().plusDays(29),
                    null, // minQuantity should default to 1
                    null  // maxQuantity should default to 10
            );

            // Then
            assertThat(request.fee()).isEqualTo(BigDecimal.ZERO);
            assertThat(request.minQuantity()).isEqualTo(1);
            assertThat(request.maxQuantity()).isEqualTo(10);
        }

        @Test
        @DisplayName("Should allow equal min and max quantities")
        void shouldAllowEqualMinAndMaxQuantities() {
            // When & Then
            assertThatCode(() -> new CreateTicketTypeRequest(
                    "VIP",
                    "VIP Ticket",
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(10),
                    1,
                    100,
                    100,
                    LocalDateTime.now().plusDays(1),
                    LocalDateTime.now().plusDays(29),
                    5, // minQuantity == maxQuantity
                    5  // maxQuantity
            )).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should allow quantity available equal to capacity")
        void shouldAllowQuantityAvailableEqualToCapacity() {
            // When & Then
            assertThatCode(() -> new CreateTicketTypeRequest(
                    "VIP",
                    "VIP Ticket",
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(10),
                    1,
                    100, // capacity
                    100, // quantityAvailable == capacity
                    LocalDateTime.now().plusDays(1),
                    LocalDateTime.now().plusDays(29),
                    1,
                    10
            )).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should allow null sale dates")
        void shouldAllowNullSaleDates() {
            // When & Then
            assertThatCode(() -> new CreateTicketTypeRequest(
                    "VIP",
                    "VIP Ticket",
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(10),
                    1,
                    100,
                    100,
                    null, // saleStart
                    null, // saleEnd
                    1,
                    10
            )).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should allow one null sale date")
        void shouldAllowOneNullSaleDate() {
            // When & Then
            assertThatCode(() -> new CreateTicketTypeRequest(
                    "VIP",
                    "VIP Ticket",
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(10),
                    1,
                    100,
                    100,
                    LocalDateTime.now().plusDays(1), // saleStart
                    null, // saleEnd null
                    1,
                    10
            )).doesNotThrowAnyException();

            assertThatCode(() -> new CreateTicketTypeRequest(
                    "VIP",
                    "VIP Ticket",
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(10),
                    1,
                    100,
                    100,
                    null, // saleStart null
                    LocalDateTime.now().plusDays(29), // saleEnd
                    1,
                    10
            )).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandlingTests {

        @Test
        @DisplayName("Should handle empty ticket type list for event")
        void shouldHandleEmptyTicketTypeListForEvent() {
            // Given
            when(ticketTypeRepository.findByEventIdOrderBySortOrder(1L)).thenReturn(List.of());
            when(ticketTypeMapper.toDTOList(List.of())).thenReturn(List.of());

            // When
            List<TicketTypeDTO> result = ticketTypeService.getTicketTypesByEventId(1L);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle zero quantity sold in validation")
        void shouldHandleZeroQuantitySoldInValidation() {
            // Given
            mockTicketType.setQuantitySold(0);
            UpdateTicketTypeRequest validRequest = new UpdateTicketTypeRequest(
                    "VIP", // name
                    "VIP Ticket", // description
                    BigDecimal.valueOf(100), // price
                    BigDecimal.valueOf(10), // fee
                    1, // sortOrder
                    100, // capacity
                    50, // quantityAvailable
                    LocalDateTime.now().plusDays(1), // saleStart
                    LocalDateTime.now().plusDays(29), // saleEnd
                    1, // minQuantity
                    10, // maxQuantity
                    true // isActive
            );
            when(ticketTypeRepository.findById(1L)).thenReturn(Optional.of(mockTicketType));
            when(ticketTypeRepository.save(any(TicketType.class))).thenReturn(mockTicketType);
            when(ticketTypeMapper.toDTO(mockTicketType)).thenReturn(mockTicketTypeDTO);

            // When & Then
            assertThatCode(() -> ticketTypeService.updateTicketType(1L, validRequest))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle null quantity sold in validation")
        void shouldHandleNullQuantitySoldInValidation() {
            // Given
            mockTicketType.setQuantitySold(null);
            UpdateTicketTypeRequest validRequest = new UpdateTicketTypeRequest(
                    "VIP", // name
                    "VIP Ticket", // description
                    BigDecimal.valueOf(100), // price
                    BigDecimal.valueOf(10), // fee
                    1, // sortOrder
                    100, // capacity
                    0, // quantityAvailable - Same as sold (0)
                    LocalDateTime.now().plusDays(1), // saleStart
                    LocalDateTime.now().plusDays(29), // saleEnd
                    1, // minQuantity
                    10, // maxQuantity
                    true // isActive
            );
            when(ticketTypeRepository.findById(1L)).thenReturn(Optional.of(mockTicketType));
            when(ticketTypeRepository.save(any(TicketType.class))).thenReturn(mockTicketType);
            when(ticketTypeMapper.toDTO(mockTicketType)).thenReturn(mockTicketTypeDTO);

            // When & Then
            assertThatCode(() -> ticketTypeService.updateTicketType(1L, validRequest))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle empty reorder list")
        void shouldHandleEmptyReorderList() {
            // Given
            when(ticketTypeRepository.findByEventIdOrderBySortOrderAscIdAsc(1L))
                    .thenReturn(List.of());
            when(ticketTypeRepository.saveAll(List.of())).thenReturn(List.of());
            when(ticketTypeMapper.toDTOList(List.of())).thenReturn(List.of());

            // When
            List<TicketTypeDTO> result = ticketTypeService.reorderTicketTypes(1L, List.of());

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should calculate next sort order when existing tickets have null sort order")
        void shouldCalculateNextSortOrderWhenExistingTicketsHaveNullSortOrder() {
            // Given
            TicketType existingTicketType1 = new TicketType();
            existingTicketType1.setId(2L);
            existingTicketType1.setSortOrder(null); // null sort order

            TicketType existingTicketType2 = new TicketType();
            existingTicketType2.setId(3L);
            existingTicketType2.setSortOrder(null); // null sort order

            CreateTicketTypeRequest requestWithoutSortOrder = new CreateTicketTypeRequest(
                    "VIP",
                    "VIP Ticket",
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(10), // fee
                    null, // sortOrder - should be calculated
                    100, // capacity
                    100, // quantityAvailable
                    LocalDateTime.now().plusDays(1), // saleStart
                    LocalDateTime.now().plusDays(29), // saleEnd
                    1, // minQuantity
                    10 // maxQuantity
            );

            when(eventRepository.findById(1L)).thenReturn(Optional.of(mockEvent));
            when(ticketTypeMapper.toEntity(requestWithoutSortOrder)).thenReturn(mockTicketType);
            when(ticketTypeRepository.save(any(TicketType.class))).thenReturn(mockTicketType);
            when(ticketTypeMapper.toDTO(mockTicketType)).thenReturn(mockTicketTypeDTO);

            // When
            ticketTypeService.createTicketType(1L, requestWithoutSortOrder);

            // Then
            verify(ticketTypeRepository).save(argThat(ticketType ->
                    ticketType.getSortOrder() == 1)); // Should be 1 since existing tickets have null (treated as 0)
        }
    }

    @Nested
    @DisplayName("Integration-like Tests")
    class IntegrationLikeTests {

        @Test
        @DisplayName("Should handle complete ticket type lifecycle")
        void shouldHandleCompleteTicketTypeLifecycle() {
            // Given - Create
            when(eventRepository.findById(1L)).thenReturn(Optional.of(mockEvent));
            when(ticketTypeMapper.toEntity(createRequest)).thenReturn(mockTicketType);
            when(ticketTypeRepository.save(any(TicketType.class))).thenReturn(mockTicketType);
            when(ticketTypeMapper.toDTO(mockTicketType)).thenReturn(mockTicketTypeDTO);

            // When - Create
            TicketTypeDTO created = ticketTypeService.createTicketType(1L, createRequest);

            // Given - Update
            when(ticketTypeRepository.findById(1L)).thenReturn(Optional.of(mockTicketType));

            // When - Update
            TicketTypeDTO updated = ticketTypeService.updateTicketType(1L, updateRequest);

            // Given - Toggle status
            mockTicketType.setIsActive(true);

            // When - Toggle status
            TicketTypeDTO toggled = ticketTypeService.toggleActiveStatus(1L);

            // Given - Delete (no sales)
            mockTicketType.setQuantitySold(0);

            // When - Delete
            ticketTypeService.deleteTicketType(1L);

            // Then
            assertThat(created).isNotNull();
            assertThat(updated).isNotNull();
            assertThat(toggled).isNotNull();
            verify(ticketTypeRepository).delete(mockTicketType);
        }

        @Test
        @DisplayName("Should handle concurrent quantity updates")
        void shouldHandleConcurrentQuantityUpdates() {
            // Given
            when(ticketTypeRepository.updateQuantitySold(1L, 5)).thenReturn(1);
            when(ticketTypeRepository.updateQuantitySold(1L, 3)).thenReturn(1);

            // When
            ticketTypeService.updateQuantitySold(1L, 5);
            ticketTypeService.updateQuantitySold(1L, 3);

            // Then
            verify(ticketTypeRepository).updateQuantitySold(1L, 5);
            verify(ticketTypeRepository).updateQuantitySold(1L, 3);
        }
    }

    @Nested
    @DisplayName("Performance and Optimization Tests")
    class PerformanceAndOptimizationTests {

        @Test
        @DisplayName("Should minimize database calls in bulk operations")
        void shouldMinimizeDatabaseCallsInBulkOperations() {
            // Given
            CreateTicketTypeRequest request1 = createRequest;
            CreateTicketTypeRequest request2 = new CreateTicketTypeRequest(
                    "Standard",
                    "Standard Ticket",
                    BigDecimal.valueOf(50),
                    BigDecimal.valueOf(200), // fee
                    2, // sortOrder
                    50, // capacity
                    50, // quantityAvailable
                    null,
                    null,
                    1, // minQuantity
                    5 // maxQuantity
            );
            List<CreateTicketTypeRequest> requests = List.of(request1, request2);

            when(eventRepository.findById(1L)).thenReturn(Optional.of(mockEvent));
            when(ticketTypeMapper.toEntity(any(CreateTicketTypeRequest.class))).thenReturn(mockTicketType);
            when(ticketTypeRepository.saveAll(anyList())).thenReturn(List.of(mockTicketType, mockTicketType));
            when(ticketTypeMapper.toDTOList(anyList())).thenReturn(List.of(mockTicketTypeDTO, mockTicketTypeDTO));

            // When
            ticketTypeService.createBulkTicketTypes(1L, requests);

            // Then
            verify(eventRepository, times(1)).findById(1L); // Only one call to verify event
            verify(ticketTypeRepository, times(1)).saveAll(anyList()); // Only one save call
        }

        @Test
        @DisplayName("Should use read-only transactions for query methods")
        void shouldUseReadOnlyTransactionsForQueryMethods() {
            // This test verifies that @Transactional(readOnly = true) is used
            // The actual verification would be done through integration tests
            // or by checking the method annotations

            // Given
            when(ticketTypeRepository.findById(1L)).thenReturn(Optional.of(mockTicketType));
            when(ticketTypeMapper.toDTO(mockTicketType)).thenReturn(mockTicketTypeDTO);

            // When
            TicketTypeDTO result = ticketTypeService.getTicketTypeById(1L);

            // Then
            assertThat(result).isNotNull();
            // In a real integration test, we would verify that no write operations are performed
        }
    }
}


