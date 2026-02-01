package com.lagab.eventz.app.domain.event.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lagab.eventz.app.domain.event.dto.ticket.TicketTypeDTO;
import com.lagab.eventz.app.domain.event.dto.ticket.category.TicketCategoryDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class TicketTypeManagementService {

    private final TicketCategoryService ticketCategoryService;
    private final TicketTypeService ticketTypeService;

    /**
     * Retrieves all ticket types organized by categories for an event
     */
    public Map<String, List<TicketTypeDTO>> getTicketTypesGroupedByCategory(Long eventId) {
        var categories = ticketCategoryService.getActiveTicketCategoriesByEventId(eventId);
        var uncategorizedTickets = ticketTypeService.getUncategorizedTicketTypes(eventId);

        Map<String, List<TicketTypeDTO>> groupedTickets = categories.stream()
                                                                    .collect(Collectors.toMap(
                                                                            TicketCategoryDTO::name,
                                                                            TicketCategoryDTO::ticketTypes
                                                                    ));

        if (!uncategorizedTickets.isEmpty()) {
            groupedTickets.put("Uncategorized", uncategorizedTickets);
        }

        return groupedTickets;
    }

    /**
     * Retrieves the complete category structure with their ticket types
     */
    public List<TicketCategoryDTO> getCompleteTicketStructure(Long eventId) {
        return ticketCategoryService.getTicketCategoriesByEventId(eventId);
    }

    /**
     * Retrieves only ticket types available for sale, organized by categories
     */
    public Map<String, List<TicketTypeDTO>> getOnSaleTicketTypesGroupedByCategory(Long eventId) {
        var categories = ticketCategoryService.getActiveTicketCategoriesByEventId(eventId);
        var onSaleTickets = ticketTypeService.getOnSaleTicketTypesByEventId(eventId);

        // Group on-sale tickets by category
        Map<Long, List<TicketTypeDTO>> ticketsByCategory = onSaleTickets.stream()
                                                                        .filter(ticket -> ticket.categoryId() != null)
                                                                        .collect(Collectors.groupingBy(TicketTypeDTO::categoryId));

        // Uncategorized on-sale tickets
        var uncategorizedOnSale = onSaleTickets.stream()
                                               .filter(ticket -> ticket.categoryId() == null)
                                               .toList();

        Map<String, List<TicketTypeDTO>> result = categories.stream()
                                                            .filter(category -> ticketsByCategory.containsKey(category.id()))
                                                            .collect(Collectors.toMap(
                                                                    TicketCategoryDTO::name,
                                                                    category -> ticketsByCategory.get(category.id())
                                                            ));

        if (!uncategorizedOnSale.isEmpty()) {
            result.put("General", uncategorizedOnSale);
        }

        return result;
    }

}
