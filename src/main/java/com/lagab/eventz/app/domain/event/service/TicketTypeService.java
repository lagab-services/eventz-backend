package com.lagab.eventz.app.domain.event.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lagab.eventz.app.common.exception.BusinessException;
import com.lagab.eventz.app.domain.event.dto.ticket.BulkUpdateTicketTypeRequest;
import com.lagab.eventz.app.domain.event.dto.ticket.CreateTicketTypeRequest;
import com.lagab.eventz.app.domain.event.dto.ticket.TicketTypeDTO;
import com.lagab.eventz.app.domain.event.dto.ticket.TicketTypeStatsDTO;
import com.lagab.eventz.app.domain.event.dto.ticket.UpdateTicketTypeRequest;
import com.lagab.eventz.app.domain.event.mapper.TicketTypeMapper;
import com.lagab.eventz.app.domain.event.model.TicketCategory;
import com.lagab.eventz.app.domain.event.model.TicketType;
import com.lagab.eventz.app.domain.event.repository.EventRepository;
import com.lagab.eventz.app.domain.event.repository.TicketCategoryRepository;
import com.lagab.eventz.app.domain.event.repository.TicketTypeRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TicketTypeService {

    public static final String TICKET_TYPE_NOT_FOUND_WITH_ID = "Ticket type not found with ID: ";
    private final TicketTypeRepository ticketTypeRepository;
    private final EventRepository eventRepository;
    private final TicketTypeMapper ticketTypeMapper;
    private final TicketCategoryRepository ticketCategoryRepository;

    public TicketTypeDTO createTicketType(Long eventId, CreateTicketTypeRequest request) {
        log.debug("Creating new ticket type: {} for event: {}", request.name(), eventId);

        // Verify the event exists
        var event = eventRepository.findById(eventId)
                                   .orElseThrow(() -> new EntityNotFoundException("Event not found with ID: " + eventId));

        // Validate sale dates against event dates
        validateSaleDates(request.saleStart(), request.saleEnd(), event.getStartDate());

        var ticketType = ticketTypeMapper.toEntity(request);
        ticketType.setEvent(event);
        // Set category if specified
        if (request.categoryId() != null) {
            var category = ticketCategoryRepository.findById(request.categoryId())
                                                   .orElseThrow(() -> new EntityNotFoundException(
                                                           "Ticket category not found with ID: " + request.categoryId()));

            // Verify category belongs to the same event
            if (!category.getEvent().getId().equals(eventId)) {
                throw new BusinessException("Category does not belong to this event");
            }

            ticketType.setCategory(category);
        }

        // Set sort order if not specified
        if (ticketType.getSortOrder() == null) {
            ticketType.setSortOrder(getNextSortOrder(eventId));
        }

        ticketType = ticketTypeRepository.save(ticketType);
        log.debug("Ticket type created successfully: {}", ticketType.getId());

        return ticketTypeMapper.toDTO(ticketType);
    }

    public List<TicketTypeDTO> createBulkTicketTypes(Long eventId, List<CreateTicketTypeRequest> requests) {
        log.debug("Bulk creating {} ticket types for event: {}", requests.size(), eventId);

        // Verify the event exists
        var event = eventRepository.findById(eventId)
                                   .orElseThrow(() -> new EntityNotFoundException("Event not found with ID: " + eventId));

        // Validate all names are unique
        var names = requests.stream().map(CreateTicketTypeRequest::name).toList();
        if (names.size() != names.stream().distinct().count()) {
            throw new BusinessException("Ticket type names must be unique");
        }

        var ticketTypes = requests.stream()
                                  .map(request -> {
                                      validateSaleDates(request.saleStart(), request.saleEnd(), event.getStartDate());
                                      var ticketType = ticketTypeMapper.toEntity(request);
                                      ticketType.setEvent(event);
                                      return ticketType;
                                  })
                                  .toList();

        ticketTypes = ticketTypeRepository.saveAll(ticketTypes);
        log.debug("Bulk creation completed: {} ticket types created", ticketTypes.size());

        return ticketTypeMapper.toDTOList(ticketTypes);
    }

    @Transactional(readOnly = true)
    public TicketTypeDTO getTicketTypeById(Long id) {
        var ticketType = ticketTypeRepository.findById(id)
                                             .orElseThrow(() -> new EntityNotFoundException(TICKET_TYPE_NOT_FOUND_WITH_ID + id));
        return ticketTypeMapper.toDTO(ticketType);
    }

    @Transactional(readOnly = true)
    public List<TicketTypeDTO> getTicketTypesByEventId(Long eventId) {
        var ticketTypes = ticketTypeRepository.findByEventIdOrderBySortOrder(eventId);
        return ticketTypeMapper.toDTOList(ticketTypes);
    }

    @Transactional(readOnly = true)
    public List<TicketTypeDTO> getActiveTicketTypesByEventId(Long eventId) {
        var ticketTypes = ticketTypeRepository.findActiveByEventId(eventId);
        return ticketTypeMapper.toDTOList(ticketTypes);
    }

    @Transactional(readOnly = true)
    public List<TicketTypeDTO> getOnSaleTicketTypesByEventId(Long eventId) {
        var ticketTypes = ticketTypeRepository.findOnSaleByEventId(eventId, LocalDateTime.now());
        return ticketTypeMapper.toDTOList(ticketTypes);
    }

    public TicketTypeDTO updateTicketType(Long id, UpdateTicketTypeRequest request) {
        log.debug("Updating ticket type: {}", id);

        var ticketType = ticketTypeRepository.findById(id)
                                             .orElseThrow(() -> new EntityNotFoundException(TICKET_TYPE_NOT_FOUND_WITH_ID + id));

        // Validate dates if modified
        var saleStart = request.saleStart() != null ? request.saleStart() : ticketType.getSaleStart();
        var saleEnd = request.saleEnd() != null ? request.saleEnd() : ticketType.getSaleEnd();
        validateSaleDates(saleStart, saleEnd, ticketType.getEvent().getStartDate());

        // Validate available quantity if modified
        if (request.quantityAvailable() != null) {
            validateQuantityAvailable(request.quantityAvailable(), ticketType.getQuantitySold());
        }

        ticketTypeMapper.updateEntityFromDTO(request, ticketType);
        ticketType = ticketTypeRepository.save(ticketType);

        log.debug("Ticket type updated successfully: {}", id);
        return ticketTypeMapper.toDTO(ticketType);
    }

    public List<TicketTypeDTO> updateBulkTicketTypes(BulkUpdateTicketTypeRequest request) {
        log.debug("Bulk updating {} ticket types", request.updates().size());

        var ticketTypes = request.updates().stream()
                                 .map(update -> {
                                     var ticketType = ticketTypeRepository.findById(update.id())
                                                                          .orElseThrow(() -> new EntityNotFoundException(
                                                                                  TICKET_TYPE_NOT_FOUND_WITH_ID + update.id()));

                                     ticketTypeMapper.updateEntityFromDTO(update.updateRequest(), ticketType);
                                     return ticketType;
                                 })
                                 .toList();

        ticketTypes = ticketTypeRepository.saveAll(ticketTypes);
        log.debug("Bulk Ticket type updated successfully");

        return ticketTypeMapper.toDTOList(ticketTypes);
    }

    public void deleteTicketType(Long id) {
        log.debug("Deleting ticket type: {}", id);

        var ticketType = ticketTypeRepository.findById(id)
                                             .orElseThrow(() -> new EntityNotFoundException(TICKET_TYPE_NOT_FOUND_WITH_ID + id));

        // Verify no tickets have been sold
        if (ticketType.getQuantitySold() > 0) {
            throw new BusinessException("Cannot delete a ticket type with existing sales");
        }

        ticketTypeRepository.delete(ticketType);
        log.debug("Ticket type deleted successfully: {}", id);
    }

    public TicketTypeDTO toggleActiveStatus(Long id) {
        log.debug("Toggling active status for ticket type: {}", id);

        var ticketType = ticketTypeRepository.findById(id)
                                             .orElseThrow(() -> new EntityNotFoundException(TICKET_TYPE_NOT_FOUND_WITH_ID + id));

        ticketType.setIsActive(!Boolean.TRUE.equals(ticketType.getIsActive()));
        ticketType = ticketTypeRepository.save(ticketType);

        log.debug("Active status toggled for ticket type: {} -> {}", id, ticketType.getIsActive());
        return ticketTypeMapper.toDTO(ticketType);
    }

    public void updateQuantitySold(Long ticketTypeId, Integer quantity) {
        log.debug("Updating sold quantity for ticket type: {} (+{})", ticketTypeId, quantity);

        var updated = ticketTypeRepository.updateQuantitySold(ticketTypeId, quantity);
        if (updated == 0) {
            throw new EntityNotFoundException(TICKET_TYPE_NOT_FOUND_WITH_ID + ticketTypeId);
        }

        log.debug("Sold quantity updated successfully");
    }

    @Transactional(readOnly = true)
    public TicketTypeStatsDTO getEventTicketTypeStats(Long eventId) {
        return ticketTypeRepository.getStatsByEventId(eventId)
                                   .map(ticketTypeMapper::toDTO)
                                   .orElse(new TicketTypeStatsDTO(0L, 0L, 0L, 0, 0, 0, BigDecimal.ZERO, 0.0, 0.0));
    }

    @Transactional(readOnly = true)
    public boolean isTicketTypeAvailable(Long ticketTypeId, Integer requestedQuantity) {
        var availableQuantity = ticketTypeRepository.getAvailableQuantity(ticketTypeId);
        return availableQuantity.map(qty -> qty >= requestedQuantity).orElse(false);
    }

    public List<TicketTypeDTO> reorderTicketTypes(Long eventId, List<Long> ticketTypeIds) {
        log.debug("Reordering ticket types for event: {}", eventId);

        var ticketTypes = ticketTypeRepository.findByEventIdOrderBySortOrderAscIdAsc(eventId);

        // Verify all IDs belong to the event
        var existingIds = ticketTypes.stream().map(TicketType::getId).toList();
        if (!existingIds.containsAll(ticketTypeIds)) {
            throw new BusinessException("Some ticket types don't belong to this event");
        }

        // Update the order
        for (int i = 0; i < ticketTypeIds.size(); i++) {
            var ticketTypeId = ticketTypeIds.get(i);
            var ticketType = ticketTypes.stream()
                                        .filter(tt -> tt.getId().equals(ticketTypeId))
                                        .findFirst()
                                        .orElseThrow();
            ticketType.setSortOrder(i + 1);
        }

        ticketTypes = ticketTypeRepository.saveAll(ticketTypes);
        log.debug("Reordering completed");

        return ticketTypeMapper.toDTOList(ticketTypes);
    }

    @Transactional(readOnly = true)
    public List<TicketTypeDTO> getUncategorizedTicketTypes(Long eventId) {
        var ticketTypes = ticketTypeRepository.findUncategorizedByEventId(eventId);
        return ticketTypeMapper.toDTOList(ticketTypes);
    }

    public List<TicketTypeDTO> moveTicketTypesToCategory(List<Long> ticketTypeIds, Long categoryId) {
        log.debug("Moving {} ticket types to category: {}", ticketTypeIds.size(), categoryId);

        TicketCategory category;
        if (categoryId != null) {
            category = ticketCategoryRepository.findById(categoryId)
                                               .orElseThrow(() -> new EntityNotFoundException("Ticket category not found with ID: " + categoryId));
        } else {
            category = null;
        }

        var ticketTypes = Stream.ofNullable(ticketTypeIds).flatMap(Collection::stream)
                                .map(id -> ticketTypeRepository.findById(id)
                                                               .orElseThrow(() -> new EntityNotFoundException(
                                                                       TICKET_TYPE_NOT_FOUND_WITH_ID + id)))
                                .toList();

        // Verify all ticket types belong to the same event as the category (if category is specified)
        if (category != null) {
            var eventId = category.getEvent().getId();
            for (var ticketType : ticketTypes) {
                if (!ticketType.getEvent().getId().equals(eventId)) {
                    throw new BusinessException("Ticket type " + ticketType.getId() + " does not belong to the same event as the category");
                }
            }
        }

        if (!ticketTypes.isEmpty()) {
            ticketTypes.getFirst().setCategory(category);
        }

        // Update category for all ticket types
        ticketTypes.forEach(tt -> tt.setCategory(category));
        ticketTypes = ticketTypeRepository.saveAll(ticketTypes);

        log.debug("Successfully moved {} ticket types to category", ticketTypes.size());
        return ticketTypeMapper.toDTOList(ticketTypes);
    }

    // Private helper methods

    private void validateSaleDates(LocalDateTime saleStart, LocalDateTime saleEnd, LocalDateTime eventStart) {
        var now = LocalDateTime.now();

        if (saleStart != null && saleStart.isBefore(now)) {
            throw new BusinessException("Sale start date cannot be in the past");
        }

        if (saleEnd != null && saleEnd.isBefore(now)) {
            throw new BusinessException("Sale end date cannot be in the past");
        }

        if (saleStart != null && saleEnd != null && saleStart.isAfter(saleEnd)) {
            throw new BusinessException("Sale start date cannot be after end date");
        }

        if (saleEnd != null && eventStart != null && saleEnd.isAfter(eventStart)) {
            throw new BusinessException("Sales must end before the event starts");
        }
    }

    private void validateQuantityAvailable(Integer newQuantity, Integer quantitySold) {
        if (newQuantity < (quantitySold != null ? quantitySold : 0)) {
            throw new BusinessException("Available quantity cannot be less than already sold quantity");
        }
    }

    private Integer getNextSortOrder(Long eventId) {
        var ticketTypes = ticketTypeRepository.findByEventIdOrderBySortOrderAscIdAsc(eventId);
        return ticketTypes.stream()
                          .mapToInt(tt -> tt.getSortOrder() != null ? tt.getSortOrder() : 0)
                          .max()
                          .orElse(0) + 1;
    }
}
