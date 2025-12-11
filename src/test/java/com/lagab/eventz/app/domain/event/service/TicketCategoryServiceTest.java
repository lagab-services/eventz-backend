package com.lagab.eventz.app.domain.event.service;

import java.util.Collections;
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
import com.lagab.eventz.app.domain.event.dto.ticket.category.CreateTicketCategoryRequest;
import com.lagab.eventz.app.domain.event.dto.ticket.category.TicketCategoryDTO;
import com.lagab.eventz.app.domain.event.dto.ticket.category.UpdateTicketCategoryRequest;
import com.lagab.eventz.app.domain.event.mapper.TicketCategoryMapper;
import com.lagab.eventz.app.domain.event.model.Event;
import com.lagab.eventz.app.domain.event.model.TicketCategory;
import com.lagab.eventz.app.domain.event.model.TicketType;
import com.lagab.eventz.app.domain.event.repository.EventRepository;
import com.lagab.eventz.app.domain.event.repository.TicketCategoryRepository;

import jakarta.persistence.EntityNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketCategoryService Tests")
class TicketCategoryServiceTest {

    @Mock
    private TicketCategoryRepository ticketCategoryRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketCategoryMapper ticketCategoryMapper;

    @InjectMocks
    private TicketCategoryService ticketCategoryService;

    private Event mockEvent;
    private TicketCategory mockCategory;
    private TicketCategoryDTO mockCategoryDTO;
    private CreateTicketCategoryRequest createRequest;
    private UpdateTicketCategoryRequest updateRequest;

    @BeforeEach
    void setUp() {
        mockEvent = createMockEvent();
        mockCategory = createMockCategory();
        mockCategoryDTO = createMockCategoryDTO();
        createRequest = new CreateTicketCategoryRequest("VIP", "VIP Category", 1, true, false);
        updateRequest = new UpdateTicketCategoryRequest("Premium VIP", "Updated description", 2, true, false);
    }

    @Nested
    @DisplayName("Create Default Category Tests")
    class CreateDefaultCategoryTests {

        @Test
        @DisplayName("Should create default category successfully")
        void shouldCreateDefaultCategorySuccessfully() {
            // Given
            String categoryName = "General";
            when(ticketCategoryRepository.save(any(TicketCategory.class))).thenReturn(mockCategory);

            // When
            TicketCategory result = ticketCategoryService.createDefaultCategory(mockEvent, categoryName);

            // Then
            assertThat(result).isNotNull();
            verify(ticketCategoryRepository).save(any(TicketCategory.class));
        }
    }

    @Nested
    @DisplayName("Create Ticket Category Tests")
    class CreateTicketCategoryTests {

        @Test
        @DisplayName("Should create ticket category successfully")
        void shouldCreateTicketCategorySuccessfully() {
            // Given
            Long eventId = 1L;
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(mockEvent));
            when(ticketCategoryRepository.existsByEventIdAndName(eventId, createRequest.name())).thenReturn(false);
            when(ticketCategoryMapper.toEntity(createRequest)).thenReturn(mockCategory);
            when(ticketCategoryRepository.save(any(TicketCategory.class))).thenReturn(mockCategory);
            when(ticketCategoryMapper.toDTO(mockCategory)).thenReturn(mockCategoryDTO);

            // When
            TicketCategoryDTO result = ticketCategoryService.createTicketCategory(eventId, createRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo(mockCategoryDTO.name());
            verify(eventRepository).findById(eventId);
            verify(ticketCategoryRepository).existsByEventIdAndName(eventId, createRequest.name());
            verify(ticketCategoryRepository).save(any(TicketCategory.class));
        }

        @Test
        @DisplayName("Should throw exception when event not found")
        void shouldThrowExceptionWhenEventNotFound() {
            // Given
            Long eventId = 999L;
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> ticketCategoryService.createTicketCategory(eventId, createRequest))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Event not found with ID: " + eventId);

            verify(ticketCategoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when category name already exists")
        void shouldThrowExceptionWhenCategoryNameAlreadyExists() {
            // Given
            Long eventId = 1L;
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(mockEvent));
            when(ticketCategoryRepository.existsByEventIdAndName(eventId, createRequest.name())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> ticketCategoryService.createTicketCategory(eventId, createRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("A category with this name already exists for this event");

            verify(ticketCategoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should set next display order when not specified")
        void shouldSetNextDisplayOrderWhenNotSpecified() {
            // Given
            Long eventId = 1L;
            CreateTicketCategoryRequest requestWithoutOrder = new CreateTicketCategoryRequest("VIP", "VIP Category", null, true, false);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(mockEvent));
            when(ticketCategoryRepository.existsByEventIdAndName(eventId, requestWithoutOrder.name())).thenReturn(false);
            mockCategory.setDisplayOrder(null);
            when(ticketCategoryMapper.toEntity(requestWithoutOrder)).thenReturn(mockCategory);

            when(ticketCategoryRepository.findMaxDisplayOrderByEventId(eventId)).thenReturn(Optional.of(3));
            when(ticketCategoryRepository.save(any(TicketCategory.class))).thenReturn(mockCategory);
            when(ticketCategoryMapper.toDTO(mockCategory)).thenReturn(mockCategoryDTO);

            // When
            ticketCategoryService.createTicketCategory(eventId, requestWithoutOrder);

            // Then
            verify(ticketCategoryRepository).findMaxDisplayOrderByEventId(eventId);
        }

        @Test
        @DisplayName("Should handle display order increment error gracefully")
        void shouldHandleDisplayOrderIncrementErrorGracefully() {
            // Given
            Long eventId = 1L;
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(mockEvent));
            when(ticketCategoryRepository.existsByEventIdAndName(eventId, createRequest.name())).thenReturn(false);
            when(ticketCategoryMapper.toEntity(createRequest)).thenReturn(mockCategory);
            when(ticketCategoryRepository.incrementDisplayOrderFrom(eventId, createRequest.displayOrder()))
                    .thenThrow(new RuntimeException("Database error"));
            when(ticketCategoryRepository.findMaxDisplayOrderByEventId(eventId)).thenReturn(Optional.of(2));
            when(ticketCategoryRepository.save(any(TicketCategory.class))).thenReturn(mockCategory);
            when(ticketCategoryMapper.toDTO(mockCategory)).thenReturn(mockCategoryDTO);

            // When
            TicketCategoryDTO result = ticketCategoryService.createTicketCategory(eventId, createRequest);

            // Then
            assertThat(result).isNotNull();
            verify(ticketCategoryRepository).findMaxDisplayOrderByEventId(eventId);
        }
    }

    @Nested
    @DisplayName("Get Ticket Category Tests")
    class GetTicketCategoryTests {

        @Test
        @DisplayName("Should get ticket category by ID successfully")
        void shouldGetTicketCategoryByIdSuccessfully() {
            // Given
            Long categoryId = 1L;
            when(ticketCategoryRepository.findByIdWithTicketTypes(categoryId)).thenReturn(Optional.of(mockCategory));
            when(ticketCategoryMapper.toDTO(mockCategory)).thenReturn(mockCategoryDTO);

            // When
            TicketCategoryDTO result = ticketCategoryService.getTicketCategoryById(categoryId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo(mockCategoryDTO.name());
            verify(ticketCategoryRepository).findByIdWithTicketTypes(categoryId);
        }

        @Test
        @DisplayName("Should throw exception when category not found")
        void shouldThrowExceptionWhenCategoryNotFound() {
            // Given
            Long categoryId = 999L;
            when(ticketCategoryRepository.findByIdWithTicketTypes(categoryId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> ticketCategoryService.getTicketCategoryById(categoryId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Ticket category not found with ID: " + categoryId);
        }

        @Test
        @DisplayName("Should get categories by event ID successfully")
        void shouldGetCategoriesByEventIdSuccessfully() {
            // Given
            Long eventId = 1L;
            List<TicketCategory> categories = List.of(mockCategory);
            List<TicketCategoryDTO> categoryDTOs = List.of(mockCategoryDTO);

            when(ticketCategoryRepository.findByEventIdWithTicketTypes(eventId)).thenReturn(categories);
            when(ticketCategoryMapper.toDTOList(categories)).thenReturn(categoryDTOs);

            // When
            List<TicketCategoryDTO> result = ticketCategoryService.getTicketCategoriesByEventId(eventId);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().name()).isEqualTo(mockCategoryDTO.name());
        }

        @Test
        @DisplayName("Should get active categories by event ID successfully")
        void shouldGetActiveCategoriesByEventIdSuccessfully() {
            // Given
            Long eventId = 1L;
            List<TicketCategory> activeCategories = List.of(mockCategory);
            List<TicketCategoryDTO> activeCategoryDTOs = List.of(mockCategoryDTO);

            when(ticketCategoryRepository.findActiveByEventIdOrderByDisplayOrder(eventId)).thenReturn(activeCategories);
            when(ticketCategoryMapper.toDTOList(activeCategories)).thenReturn(activeCategoryDTOs);

            // When
            List<TicketCategoryDTO> result = ticketCategoryService.getActiveTicketCategoriesByEventId(eventId);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().name()).isEqualTo(mockCategoryDTO.name());
        }
    }

    @Nested
    @DisplayName("Update Ticket Category Tests")
    class UpdateTicketCategoryTests {

        @Test
        @DisplayName("Should update ticket category successfully")
        void shouldUpdateTicketCategorySuccessfully() {
            // Given
            Long categoryId = 1L;
            when(ticketCategoryRepository.findById(categoryId)).thenReturn(Optional.of(mockCategory));
            when(ticketCategoryRepository.save(any(TicketCategory.class))).thenReturn(mockCategory);
            when(ticketCategoryMapper.toDTO(mockCategory)).thenReturn(mockCategoryDTO);

            // When
            TicketCategoryDTO result = ticketCategoryService.updateTicketCategory(categoryId, updateRequest);

            // Then
            assertThat(result).isNotNull();
            verify(ticketCategoryMapper).updateEntityFromDTO(updateRequest, mockCategory);
            verify(ticketCategoryRepository).save(mockCategory);
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent category")
        void shouldThrowExceptionWhenUpdatingNonExistentCategory() {
            // Given
            Long categoryId = 999L;
            when(ticketCategoryRepository.findById(categoryId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> ticketCategoryService.updateTicketCategory(categoryId, updateRequest))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Ticket category not found with ID: " + categoryId);
        }

        @Test
        @DisplayName("Should throw exception when name conflicts with existing category")
        void shouldThrowExceptionWhenNameConflictsWithExistingCategory() {
            // Given
            Long categoryId = 1L;
            mockCategory.setName("Old Name");
            mockCategory.getEvent().setId(1L);

            when(ticketCategoryRepository.findById(categoryId)).thenReturn(Optional.of(mockCategory));
            when(ticketCategoryRepository.existsByEventIdAndNameAndIdNot(1L, updateRequest.name(), categoryId))
                    .thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> ticketCategoryService.updateTicketCategory(categoryId, updateRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("A category with this name already exists for this event");
        }
    }

    @Nested
    @DisplayName("Delete Ticket Category Tests")
    class DeleteTicketCategoryTests {

        @Test
        @DisplayName("Should delete empty category successfully")
        void shouldDeleteEmptyCategorySuccessfully() {
            // Given
            Long categoryId = 1L;
            mockCategory.setTicketTypes(Collections.emptyList());
            when(ticketCategoryRepository.findByIdWithTicketTypes(categoryId)).thenReturn(Optional.of(mockCategory));

            // When
            ticketCategoryService.deleteTicketCategory(categoryId);

            // Then
            verify(ticketCategoryRepository).delete(mockCategory);
        }

        @Test
        @DisplayName("Should delete category with ticket types without sales")
        void shouldDeleteCategoryWithTicketTypesWithoutSales() {
            // Given
            Long categoryId = 1L;
            TicketType ticketType = new TicketType();
            ticketType.setQuantitySold(0);
            mockCategory.setTicketTypes(List.of(ticketType));
            when(ticketCategoryRepository.findByIdWithTicketTypes(categoryId)).thenReturn(Optional.of(mockCategory));

            // When
            ticketCategoryService.deleteTicketCategory(categoryId);

            // Then
            verify(ticketCategoryRepository).delete(mockCategory);
        }

        @Test
        @DisplayName("Should throw exception when deleting category with sales")
        void shouldThrowExceptionWhenDeletingCategoryWithSales() {
            // Given
            Long categoryId = 1L;
            TicketType ticketTypeWithSales = new TicketType();
            ticketTypeWithSales.setQuantitySold(5);
            mockCategory.setTicketTypes(List.of(ticketTypeWithSales));
            when(ticketCategoryRepository.findByIdWithTicketTypes(categoryId)).thenReturn(Optional.of(mockCategory));

            // When & Then
            assertThatThrownBy(() -> ticketCategoryService.deleteTicketCategory(categoryId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Cannot delete a category that contains ticket types with existing sales");

            verify(ticketCategoryRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw exception when category not found for deletion")
        void shouldThrowExceptionWhenCategoryNotFoundForDeletion() {
            // Given
            Long categoryId = 999L;
            when(ticketCategoryRepository.findByIdWithTicketTypes(categoryId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> ticketCategoryService.deleteTicketCategory(categoryId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Ticket category not found with ID: " + categoryId);
        }
    }

    @Nested
    @DisplayName("Toggle Status Tests")
    class ToggleStatusTests {

        @Test
        @DisplayName("Should toggle active status from true to false")
        void shouldToggleActiveStatusFromTrueToFalse() {
            // Given
            Long categoryId = 1L;
            mockCategory.setIsActive(true);
            when(ticketCategoryRepository.findById(categoryId)).thenReturn(Optional.of(mockCategory));
            when(ticketCategoryRepository.save(any(TicketCategory.class))).thenReturn(mockCategory);
            when(ticketCategoryMapper.toDTO(mockCategory)).thenReturn(mockCategoryDTO);

            // When
            TicketCategoryDTO result = ticketCategoryService.toggleActiveStatus(categoryId);

            // Then
            assertThat(result).isNotNull();
            verify(ticketCategoryRepository).save(mockCategory);
            // Verify that setIsActive was called with false
        }

        @Test
        @DisplayName("Should toggle active status from false to true")
        void shouldToggleActiveStatusFromFalseToTrue() {
            // Given
            Long categoryId = 1L;
            mockCategory.setIsActive(false);
            when(ticketCategoryRepository.findById(categoryId)).thenReturn(Optional.of(mockCategory));
            when(ticketCategoryRepository.save(any(TicketCategory.class))).thenReturn(mockCategory);
            when(ticketCategoryMapper.toDTO(mockCategory)).thenReturn(mockCategoryDTO);

            // When
            TicketCategoryDTO result = ticketCategoryService.toggleActiveStatus(categoryId);

            // Then
            assertThat(result).isNotNull();
            verify(ticketCategoryRepository).save(mockCategory);
        }

        @Test
        @DisplayName("Should toggle collapse status successfully")
        void shouldToggleCollapseStatusSuccessfully() {
            // Given
            Long categoryId = 1L;
            mockCategory.setIsCollapsed(false);
            when(ticketCategoryRepository.findById(categoryId)).thenReturn(Optional.of(mockCategory));
            when(ticketCategoryRepository.save(any(TicketCategory.class))).thenReturn(mockCategory);
            when(ticketCategoryMapper.toDTO(mockCategory)).thenReturn(mockCategoryDTO);

            // When
            TicketCategoryDTO result = ticketCategoryService.toggleCollapseStatus(categoryId);

            // Then
            assertThat(result).isNotNull();
            verify(ticketCategoryRepository).save(mockCategory);
        }

        @Test
        @DisplayName("Should throw exception when toggling status of non-existent category")
        void shouldThrowExceptionWhenTogglingStatusOfNonExistentCategory() {
            // Given
            Long categoryId = 999L;
            when(ticketCategoryRepository.findById(categoryId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> ticketCategoryService.toggleActiveStatus(categoryId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Ticket category not found with ID: " + categoryId);
        }
    }

    @Nested
    @DisplayName("Reorder Categories Tests")
    class ReorderCategoriesTests {

        @Test
        @DisplayName("Should reorder categories successfully")
        void shouldReorderCategoriesSuccessfully() {
            // Given
            Long eventId = 1L;
            TicketCategory category1 = createMockCategoryWithId(1L);
            TicketCategory category2 = createMockCategoryWithId(2L);
            TicketCategory category3 = createMockCategoryWithId(3L);

            List<TicketCategory> categories = List.of(category1, category2, category3);
            List<Long> newOrder = List.of(3L, 1L, 2L);
            List<TicketCategoryDTO> reorderedDTOs = List.of(mockCategoryDTO, mockCategoryDTO, mockCategoryDTO);

            when(ticketCategoryRepository.findByEventIdOrderByDisplayOrderAscIdAsc(eventId)).thenReturn(categories);
            when(ticketCategoryRepository.saveAll(any())).thenReturn(categories);
            when(ticketCategoryMapper.toDTOList(categories)).thenReturn(reorderedDTOs);

            // When
            List<TicketCategoryDTO> result = ticketCategoryService.reorderTicketCategories(eventId, newOrder);

            // Then
            assertThat(result).hasSize(3);
            verify(ticketCategoryRepository).saveAll(categories);
        }

        @Test
        @DisplayName("Should throw exception when category doesn't belong to event")
        void shouldThrowExceptionWhenCategoryDoesntBelongToEvent() {
            // Given
            Long eventId = 1L;
            TicketCategory category1 = createMockCategoryWithId(1L);
            List<TicketCategory> categories = List.of(category1);
            List<Long> invalidOrder = List.of(1L, 999L); // 999L doesn't exist

            when(ticketCategoryRepository.findByEventIdOrderByDisplayOrderAscIdAsc(eventId)).thenReturn(categories);

            // When & Then
            assertThatThrownBy(() -> ticketCategoryService.reorderTicketCategories(eventId, invalidOrder))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Some ticket categories don't belong to this event");
        }
    }

    // Helper methods
    private Event createMockEvent() {
        Event event = new Event();
        event.setId(1L);
        event.setName("Test Event");
        return event;
    }

    private TicketCategory createMockCategory() {
        TicketCategory category = new TicketCategory();
        category.setId(1L);
        category.setName("VIP");
        category.setDescription("VIP Category");
        category.setDisplayOrder(1);
        category.setIsActive(true);
        category.setIsCollapsed(false);
        category.setEvent(mockEvent);
        category.setTicketTypes(Collections.emptyList());
        return category;
    }

    private TicketCategory createMockCategoryWithId(Long id) {
        TicketCategory category = new TicketCategory();
        category.setId(id);
        category.setName("Category " + id);
        category.setDisplayOrder(id.intValue());
        return category;
    }

    private TicketCategoryDTO createMockCategoryDTO() {
        return new TicketCategoryDTO(
                1L,
                "VIP",
                "VIP Category",
                1,
                true,
                false,
                Collections.emptyList()
        );
    }
}
