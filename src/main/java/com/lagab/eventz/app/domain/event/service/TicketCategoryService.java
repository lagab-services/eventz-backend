package com.lagab.eventz.app.domain.event.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lagab.eventz.app.common.exception.BusinessException;
import com.lagab.eventz.app.domain.event.dto.ticket.category.CreateTicketCategoryRequest;
import com.lagab.eventz.app.domain.event.dto.ticket.category.TicketCategoryDTO;
import com.lagab.eventz.app.domain.event.dto.ticket.category.UpdateTicketCategoryRequest;
import com.lagab.eventz.app.domain.event.mapper.TicketCategoryMapper;
import com.lagab.eventz.app.domain.event.model.Event;
import com.lagab.eventz.app.domain.event.model.TicketCategory;
import com.lagab.eventz.app.domain.event.repository.EventRepository;
import com.lagab.eventz.app.domain.event.repository.TicketCategoryRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketCategoryService {

    public static final String TICKET_CATEGORY_NOT_FOUND_WITH_ID = "Ticket category not found with ID: ";
    private final TicketCategoryRepository ticketCategoryRepository;
    private final EventRepository eventRepository;
    private final TicketCategoryMapper ticketCategoryMapper;

    public TicketCategory createDefaultCategory(Event event, String name) {
        TicketCategory category = new TicketCategory();
        category.setName(name);
        category.setEvent(event);
        return ticketCategoryRepository.save(category);
    }

    public TicketCategoryDTO createTicketCategory(Long eventId, CreateTicketCategoryRequest request) {
        log.debug("Creating new ticket category: {} for event: {}", request.name(), eventId);

        // Verify the event exists
        var event = eventRepository.findById(eventId)
                                   .orElseThrow(() -> new EntityNotFoundException("Event not found with ID: " + eventId));

        // Check if category name already exists for this event
        if (ticketCategoryRepository.existsByEventIdAndName(eventId, request.name())) {
            throw new BusinessException("A category with this name already exists for this event");
        }

        var ticketCategory = ticketCategoryMapper.toEntity(request);
        ticketCategory.setEvent(event);

        // Set display order if not specified
        if (ticketCategory.getDisplayOrder() == null || ticketCategory.getDisplayOrder() == 0) {
            ticketCategory.setDisplayOrder(getNextDisplayOrder(eventId));
        } else {
            // Increment existing categories with same or higher order
            try {
                int updatedRows = ticketCategoryRepository.incrementDisplayOrderFrom(eventId, ticketCategory.getDisplayOrder());
                log.debug("Updated {} categories display order", updatedRows);
            } catch (Exception e) {
                log.error("Error updating display order: {}", e.getMessage());
                // Fallback: set to next available order
                ticketCategory.setDisplayOrder(getNextDisplayOrder(eventId));
            }

        }

        ticketCategory = ticketCategoryRepository.save(ticketCategory);
        log.debug("Ticket category created successfully: {}", ticketCategory.getId());

        return ticketCategoryMapper.toDTO(ticketCategory);
    }

    @Transactional(readOnly = true)
    public TicketCategoryDTO getTicketCategoryById(Long id) {
        var ticketCategory = ticketCategoryRepository.findByIdWithTicketTypes(id)
                                                     .orElseThrow(() -> new EntityNotFoundException(TICKET_CATEGORY_NOT_FOUND_WITH_ID + id));
        return ticketCategoryMapper.toDTO(ticketCategory);
    }

    @Transactional(readOnly = true)
    public List<TicketCategoryDTO> getTicketCategoriesByEventId(Long eventId) {
        var ticketCategories = ticketCategoryRepository.findByEventIdWithTicketTypes(eventId);
        return ticketCategoryMapper.toDTOList(ticketCategories);
    }

    @Transactional(readOnly = true)
    public List<TicketCategoryDTO> getActiveTicketCategoriesByEventId(Long eventId) {
        var ticketCategories = ticketCategoryRepository.findActiveByEventIdOrderByDisplayOrder(eventId);
        return ticketCategoryMapper.toDTOList(ticketCategories);
    }

    public TicketCategoryDTO updateTicketCategory(Long id, UpdateTicketCategoryRequest request) {
        log.debug("Updating ticket category: {}", id);

        var ticketCategory = ticketCategoryRepository.findById(id)
                                                     .orElseThrow(() -> new EntityNotFoundException(TICKET_CATEGORY_NOT_FOUND_WITH_ID + id));

        // Check if name is being changed and if it conflicts
        if (request.name() != null && !request.name().equals(ticketCategory.getName()) && ticketCategoryRepository.existsByEventIdAndNameAndIdNot(
                ticketCategory.getEvent().getId(), request.name(), id)) {
            throw new BusinessException("A category with this name already exists for this event");
        }

        ticketCategoryMapper.updateEntityFromDTO(request, ticketCategory);
        ticketCategory = ticketCategoryRepository.save(ticketCategory);

        log.debug("Ticket category updated successfully: {}", id);
        return ticketCategoryMapper.toDTO(ticketCategory);
    }

    public void deleteTicketCategory(Long id) {
        log.debug("Deleting ticket category: {}", id);

        var ticketCategory = ticketCategoryRepository.findByIdWithTicketTypes(id)
                                                     .orElseThrow(() -> new EntityNotFoundException(TICKET_CATEGORY_NOT_FOUND_WITH_ID + id));
        // Check if category has ticket types
        if (ticketCategory.getTicketTypes() != null && !ticketCategory.getTicketTypes().isEmpty()) {
            // Check if any ticket type has sales
            boolean hasSales = ticketCategory.getTicketTypes().stream()
                                             .anyMatch(tt -> tt.getQuantitySold() != null && tt.getQuantitySold() > 0);

            if (hasSales) {
                throw new BusinessException("Cannot delete a category that contains ticket types with existing sales");
            }
        }

        ticketCategoryRepository.delete(ticketCategory);
        log.debug("Ticket category deleted successfully: {}", id);
    }

    public TicketCategoryDTO toggleActiveStatus(Long id) {
        log.debug("Toggling active status for ticket category: {}", id);

        var ticketCategory = ticketCategoryRepository.findById(id)
                                                     .orElseThrow(() -> new EntityNotFoundException(TICKET_CATEGORY_NOT_FOUND_WITH_ID + id));

        ticketCategory.setIsActive(!Boolean.TRUE.equals(ticketCategory.getIsActive()));
        ticketCategory = ticketCategoryRepository.save(ticketCategory);

        log.debug("Active status toggled for ticket category: {} -> {}", id, ticketCategory.getIsActive());
        return ticketCategoryMapper.toDTO(ticketCategory);
    }

    public TicketCategoryDTO toggleCollapseStatus(Long id) {
        log.debug("Toggling collapse status for ticket category: {}", id);

        var ticketCategory = ticketCategoryRepository.findById(id)
                                                     .orElseThrow(() -> new EntityNotFoundException(TICKET_CATEGORY_NOT_FOUND_WITH_ID + id));

        ticketCategory.setIsCollapsed(!Boolean.TRUE.equals(ticketCategory.getIsCollapsed()));
        ticketCategory = ticketCategoryRepository.save(ticketCategory);

        log.debug("Collapse status toggled for ticket category: {} -> {}", id, ticketCategory.getIsCollapsed());
        return ticketCategoryMapper.toDTO(ticketCategory);
    }

    public List<TicketCategoryDTO> reorderTicketCategories(Long eventId, List<Long> categoryIds) {
        log.debug("Reordering ticket categories for event: {}", eventId);

        var ticketCategories = ticketCategoryRepository.findByEventIdOrderByDisplayOrderAscIdAsc(eventId);

        // Verify all IDs belong to the event
        var existingIds = ticketCategories.stream().map(TicketCategory::getId).toList();
        if (!existingIds.containsAll(categoryIds)) {
            throw new BusinessException("Some ticket categories don't belong to this event");
        }

        // Update the order
        for (int i = 0; i < categoryIds.size(); i++) {
            var categoryId = categoryIds.get(i);
            var ticketCategory = ticketCategories.stream()
                                                 .filter(tc -> tc.getId().equals(categoryId))
                                                 .findFirst()
                                                 .orElseThrow();
            ticketCategory.setDisplayOrder(i + 1);
        }

        ticketCategories = ticketCategoryRepository.saveAll(ticketCategories);
        log.debug("Reordering completed");

        return ticketCategoryMapper.toDTOList(ticketCategories);
    }

    // Private helper methods

    private Integer getNextDisplayOrder(Long eventId) {
        return ticketCategoryRepository.findMaxDisplayOrderByEventId(eventId)
                                       .map(order -> order + 1)
                                       .orElse(1);
    }
}
