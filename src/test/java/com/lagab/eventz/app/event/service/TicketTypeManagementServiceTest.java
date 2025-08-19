package com.lagab.eventz.app.event.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lagab.eventz.app.domain.event.dto.ticket.TicketTypeDTO;
import com.lagab.eventz.app.domain.event.dto.ticket.category.TicketCategoryDTO;
import com.lagab.eventz.app.domain.event.service.TicketCategoryService;
import com.lagab.eventz.app.domain.event.service.TicketTypeManagementService;
import com.lagab.eventz.app.domain.event.service.TicketTypeService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketTypeManagementService Tests")
class TicketTypeManagementServiceTest {

    @Mock
    private TicketCategoryService ticketCategoryService;

    @Mock
    private TicketTypeService ticketTypeService;

    @InjectMocks
    private TicketTypeManagementService ticketTypeManagementService;

    private Long eventId;
    private TicketCategoryDTO vipCategory;
    private TicketCategoryDTO generalCategory;
    private TicketTypeDTO uncategorizedTicket;

    @BeforeEach
    void setUp() {
        eventId = 1L;
        setupMockData();
    }

    @Nested
    @DisplayName("Get Ticket Types Grouped By Category Tests")
    class GetTicketTypesGroupedByCategoryTests {

        @Test
        @DisplayName("Should group tickets by categories successfully")
        void shouldGroupTicketsByCategoriesSuccessfully() {
            // Given
            List<TicketCategoryDTO> categories = List.of(vipCategory, generalCategory);
            List<TicketTypeDTO> uncategorizedTickets = List.of(uncategorizedTicket);

            when(ticketCategoryService.getActiveTicketCategoriesByEventId(eventId)).thenReturn(categories);
            when(ticketTypeService.getUncategorizedTicketTypes(eventId)).thenReturn(uncategorizedTickets);

            // When
            Map<String, List<TicketTypeDTO>> result = ticketTypeManagementService.getTicketTypesGroupedByCategory(eventId);

            // Then
            assertThat(result).hasSize(3) // 2 categories + uncategorized
                              .containsKey("VIP")
                              .containsKey("General")
                              .containsKey("Uncategorized");

            assertThat(result.get("VIP")).hasSize(1);
            assertThat(result.get("VIP").getFirst().name()).isEqualTo("VIP Ticket");

            assertThat(result.get("General")).hasSize(1);
            assertThat(result.get("General").getFirst().name()).isEqualTo("General Ticket");

            assertThat(result.get("Uncategorized")).hasSize(1);
            assertThat(result.get("Uncategorized").getFirst().name()).isEqualTo("Uncategorized Ticket");
        }

        @Test
        @DisplayName("Should handle empty categories")
        void shouldHandleEmptyCategories() {
            // Given
            List<TicketCategoryDTO> emptyCategories = Collections.emptyList();
            List<TicketTypeDTO> uncategorizedTickets = List.of(uncategorizedTicket);

            when(ticketCategoryService.getActiveTicketCategoriesByEventId(eventId)).thenReturn(emptyCategories);
            when(ticketTypeService.getUncategorizedTicketTypes(eventId)).thenReturn(uncategorizedTickets);

            // When
            Map<String, List<TicketTypeDTO>> result = ticketTypeManagementService.getTicketTypesGroupedByCategory(eventId);

            // Then
            assertThat(result).hasSize(1)
                              .containsKey("Uncategorized");
            assertThat(result.get("Uncategorized")).hasSize(1);
        }

        @Test
        @DisplayName("Should handle no uncategorized tickets")
        void shouldHandleNoUncategorizedTickets() {
            // Given
            List<TicketCategoryDTO> categories = List.of(vipCategory);
            List<TicketTypeDTO> emptyUncategorized = Collections.emptyList();

            when(ticketCategoryService.getActiveTicketCategoriesByEventId(eventId)).thenReturn(categories);
            when(ticketTypeService.getUncategorizedTicketTypes(eventId)).thenReturn(emptyUncategorized);

            // When
            Map<String, List<TicketTypeDTO>> result = ticketTypeManagementService.getTicketTypesGroupedByCategory(eventId);

            // Then
            assertThat(result).hasSize(1)
                              .containsKey("VIP")
                              .doesNotContainKey("Uncategorized");
        }

        @Test
        @DisplayName("Should handle completely empty event")
        void shouldHandleCompletelyEmptyEvent() {
            // Given
            List<TicketCategoryDTO> emptyCategories = Collections.emptyList();
            List<TicketTypeDTO> emptyUncategorized = Collections.emptyList();

            when(ticketCategoryService.getActiveTicketCategoriesByEventId(eventId)).thenReturn(emptyCategories);
            when(ticketTypeService.getUncategorizedTicketTypes(eventId)).thenReturn(emptyUncategorized);

            // When
            Map<String, List<TicketTypeDTO>> result = ticketTypeManagementService.getTicketTypesGroupedByCategory(eventId);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Complete Ticket Structure Tests")
    class GetCompleteTicketStructureTests {

        @Test
        @DisplayName("Should return complete ticket structure")
        void shouldReturnCompleteTicketStructure() {
            // Given
            List<TicketCategoryDTO> expectedCategories = List.of(vipCategory, generalCategory);
            when(ticketCategoryService.getTicketCategoriesByEventId(eventId)).thenReturn(expectedCategories);

            // When
            List<TicketCategoryDTO> result = ticketTypeManagementService.getCompleteTicketStructure(eventId);

            // Then
            assertThat(result).hasSize(2)
                              .containsExactlyElementsOf(expectedCategories);
        }

        @Test
        @DisplayName("Should return empty list when no categories exist")
        void shouldReturnEmptyListWhenNoCategoriesExist() {
            // Given
            List<TicketCategoryDTO> emptyCategories = Collections.emptyList();
            when(ticketCategoryService.getTicketCategoriesByEventId(eventId)).thenReturn(emptyCategories);

            // When
            List<TicketCategoryDTO> result = ticketTypeManagementService.getCompleteTicketStructure(eventId);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get On Sale Ticket Types Grouped By Category Tests")
    class GetOnSaleTicketTypesGroupedByCategoryTests {

        @Test
        @DisplayName("Should group on-sale tickets by categories successfully")
        void shouldGroupOnSaleTicketsByCategoriesSuccessfully() {
            // Given
            TicketTypeDTO onSaleVipTicket = createTicketTypeDTO(1L, "VIP On Sale", 1L, "VIP");
            TicketTypeDTO onSaleGeneralTicket = createTicketTypeDTO(2L, "General On Sale", 2L, "General");
            TicketTypeDTO onSaleUncategorizedTicket = createTicketTypeDTO(3L, "Uncategorized On Sale", null, null);

            List<TicketCategoryDTO> categories = List.of(vipCategory, generalCategory);
            List<TicketTypeDTO> onSaleTickets = List.of(onSaleVipTicket, onSaleGeneralTicket, onSaleUncategorizedTicket);

            when(ticketCategoryService.getActiveTicketCategoriesByEventId(eventId)).thenReturn(categories);
            when(ticketTypeService.getOnSaleTicketTypesByEventId(eventId)).thenReturn(onSaleTickets);

            // When
            Map<String, List<TicketTypeDTO>> result = ticketTypeManagementService.getOnSaleTicketTypesGroupedByCategory(eventId);

            // Then
            assertThat(result).hasSize(2) // VIP, General, General (uncategorized)
                              .containsKey("VIP")
                              .containsKey("General");

            assertThat(result.get("VIP")).hasSize(1);
            assertThat(result.get("VIP").getFirst().name()).isEqualTo("VIP On Sale");

            assertThat(result.get("General")).hasSize(1); // One from category + one uncategorized
        }

        @Test
        @DisplayName("Should handle only categorized on-sale tickets")
        void shouldHandleOnlyCategorizedOnSaleTickets() {
            // Given
            TicketTypeDTO onSaleVipTicket = createTicketTypeDTO(1L, "VIP On Sale", 1L, "VIP");

            List<TicketCategoryDTO> categories = List.of(vipCategory);
            List<TicketTypeDTO> onSaleTickets = List.of(onSaleVipTicket);

            when(ticketCategoryService.getActiveTicketCategoriesByEventId(eventId)).thenReturn(categories);
            when(ticketTypeService.getOnSaleTicketTypesByEventId(eventId)).thenReturn(onSaleTickets);

            // When
            Map<String, List<TicketTypeDTO>> result = ticketTypeManagementService.getOnSaleTicketTypesGroupedByCategory(eventId);

            // Then
            assertThat(result).hasSize(1)
                              .containsKey("VIP")
                              .doesNotContainKey("General");
        }

        @Test
        @DisplayName("Should handle only uncategorized on-sale tickets")
        void shouldHandleOnlyUncategorizedOnSaleTickets() {
            // Given
            TicketTypeDTO onSaleUncategorizedTicket = createTicketTypeDTO(1L, "Uncategorized On Sale", null, null);

            List<TicketCategoryDTO> categories = List.of(vipCategory);
            List<TicketTypeDTO> onSaleTickets = List.of(onSaleUncategorizedTicket);

            when(ticketCategoryService.getActiveTicketCategoriesByEventId(eventId)).thenReturn(categories);
            when(ticketTypeService.getOnSaleTicketTypesByEventId(eventId)).thenReturn(onSaleTickets);

            // When
            Map<String, List<TicketTypeDTO>> result = ticketTypeManagementService.getOnSaleTicketTypesGroupedByCategory(eventId);

            // Then
            assertThat(result).hasSize(1)
                              .containsKey("General");
            assertThat(result.get("General")).hasSize(1);
            assertThat(result.get("General").getFirst().name()).isEqualTo("Uncategorized On Sale");
        }

        @Test
        @DisplayName("Should handle no on-sale tickets")
        void shouldHandleNoOnSaleTickets() {
            // Given
            List<TicketCategoryDTO> categories = List.of(vipCategory, generalCategory);
            List<TicketTypeDTO> emptyOnSaleTickets = Collections.emptyList();

            when(ticketCategoryService.getActiveTicketCategoriesByEventId(eventId)).thenReturn(categories);
            when(ticketTypeService.getOnSaleTicketTypesByEventId(eventId)).thenReturn(emptyOnSaleTickets);

            // When
            Map<String, List<TicketTypeDTO>> result = ticketTypeManagementService.getOnSaleTicketTypesGroupedByCategory(eventId);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle categories without matching on-sale tickets")
        void shouldHandleCategoriesWithoutMatchingOnSaleTickets() {
            // Given
            TicketTypeDTO onSaleUncategorizedTicket = createTicketTypeDTO(1L, "Uncategorized On Sale", null, null);

            List<TicketCategoryDTO> categories = List.of(vipCategory, generalCategory);
            List<TicketTypeDTO> onSaleTickets = List.of(onSaleUncategorizedTicket);

            when(ticketCategoryService.getActiveTicketCategoriesByEventId(eventId)).thenReturn(categories);
            when(ticketTypeService.getOnSaleTicketTypesByEventId(eventId)).thenReturn(onSaleTickets);

            // When
            Map<String, List<TicketTypeDTO>> result = ticketTypeManagementService.getOnSaleTicketTypesGroupedByCategory(eventId);

            // Then
            assertThat(result).hasSize(1)
                              .containsKey("General")
                              .doesNotContainKey("VIP");
        }
    }

    // Helper methods
    private void setupMockData() {
        TicketTypeDTO vipTicket = createTicketTypeDTO(1L, "VIP Ticket", 1L, "VIP");
        TicketTypeDTO generalTicket = createTicketTypeDTO(2L, "General Ticket", 2L, "General");
        uncategorizedTicket = createTicketTypeDTO(3L, "Uncategorized Ticket", null, null);

        vipCategory = new TicketCategoryDTO(
                1L,
                "VIP",
                "VIP Category",
                1,
                true,
                false,
                List.of(vipTicket)
        );

        generalCategory = new TicketCategoryDTO(
                2L,
                "General",
                "General Category",
                2,
                true,
                false,
                List.of(generalTicket)
        );
    }

    private TicketTypeDTO createTicketTypeDTO(Long id, String name, Long categoryId, String categoryName) {
        return createTicketTypeDTO(id, name, categoryId, categoryName, 100, 10, true);
    }

    private TicketTypeDTO createTicketTypeDTO(Long id, String name, Long categoryId, String categoryName,
            Integer quantityAvailable, Integer quantitySold, Boolean isActive) {
        int remaining = quantityAvailable - quantitySold;
        BigDecimal price = BigDecimal.valueOf(100.00);
        BigDecimal totalPrice = price.multiply(BigDecimal.valueOf(quantitySold));

        return new TicketTypeDTO(
                id,                                    // id
                name,                                  // name
                "Description for " + name,             // description
                price,                                 // price
                quantityAvailable,                     // quantityAvailable
                quantitySold,                          // quantitySold
                LocalDateTime.now().minusDays(1),      // saleStart
                LocalDateTime.now().plusDays(30),      // saleEnd
                1,                                     // minQuantity
                10,                                    // maxQuantity
                isActive,                              // isActive
                remaining,                             // remainingTickets (deprecated)
                categoryId,                            // categoryId
                categoryName,                          // categoryName
                remaining,                             // quantityRemaining (computed)
                totalPrice,                            // totalPrice (computed)
                isActive && remaining > 0,             // isOnSale (computed)
                remaining <= 0,                         // isSoldOut (computed)
                1L,                                     // event ID
                "event Name"                            // event Name
        );
    }
}
