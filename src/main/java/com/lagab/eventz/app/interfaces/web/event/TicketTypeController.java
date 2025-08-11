package com.lagab.eventz.app.interfaces.web.event;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lagab.eventz.app.domain.event.dto.ticket.BulkUpdateTicketTypeRequest;
import com.lagab.eventz.app.domain.event.dto.ticket.CreateTicketTypeRequest;
import com.lagab.eventz.app.domain.event.dto.ticket.TicketTypeDTO;
import com.lagab.eventz.app.domain.event.dto.ticket.TicketTypeStatsDTO;
import com.lagab.eventz.app.domain.event.dto.ticket.UpdateTicketTypeRequest;
import com.lagab.eventz.app.domain.event.dto.ticket.category.CreateTicketCategoryRequest;
import com.lagab.eventz.app.domain.event.dto.ticket.category.TicketCategoryDTO;
import com.lagab.eventz.app.domain.event.service.TicketCategoryService;
import com.lagab.eventz.app.domain.event.service.TicketTypeManagementService;
import com.lagab.eventz.app.domain.event.service.TicketTypeService;
import com.lagab.eventz.app.interfaces.web.org.annotation.RequireOrganizationPermission;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/ticket-types")
@RequiredArgsConstructor
@Tag(name = "Ticket Types", description = "Ticket type management API")
public class TicketTypeController {

    private final TicketTypeService ticketTypeService;
    private final TicketCategoryService ticketCategoryService;
    private final TicketTypeManagementService ticketTypeManagementService;

    @PostMapping("/event/{eventId}")
    @Operation(summary = "Create a new ticket type")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<TicketTypeDTO> createTicketType(@PathVariable Long eventId, @Valid @RequestBody CreateTicketTypeRequest request) {
        var ticketType = ticketTypeService.createTicketType(eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketType);
    }

    @PostMapping("/bulk")
    @Operation(summary = "Create multiple ticket types at once")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<List<TicketTypeDTO>> createBulkTicketTypes(
            @Parameter(description = "Event ID") @RequestParam Long eventId,
            @Valid @RequestBody List<CreateTicketTypeRequest> requests) {
        var ticketTypes = ticketTypeService.createBulkTicketTypes(eventId, requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketTypes);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a ticket type by ID")
    public ResponseEntity<TicketTypeDTO> getTicketTypeById(@PathVariable Long id) {
        var ticketType = ticketTypeService.getTicketTypeById(id);
        return ResponseEntity.ok(ticketType);
    }

    @GetMapping("/event/{eventId}")
    @Operation(summary = "Get all ticket types for an event")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<List<TicketTypeDTO>> getTicketTypesByEventId(@PathVariable Long eventId) {
        var ticketTypes = ticketTypeService.getTicketTypesByEventId(eventId);
        return ResponseEntity.ok(ticketTypes);
    }

    @GetMapping("/event/{eventId}/active")
    @Operation(summary = "Get active ticket types for an event")
    public ResponseEntity<List<TicketTypeDTO>> getActiveTicketTypesByEventId(@PathVariable Long eventId) {
        var ticketTypes = ticketTypeService.getActiveTicketTypesByEventId(eventId);
        return ResponseEntity.ok(ticketTypes);
    }

    @GetMapping("/event/{eventId}/on-sale")
    @Operation(summary = "Get ticket types currently on sale for an event")
    public ResponseEntity<List<TicketTypeDTO>> getOnSaleTicketTypesByEventId(@PathVariable Long eventId) {
        var ticketTypes = ticketTypeService.getOnSaleTicketTypesByEventId(eventId);
        return ResponseEntity.ok(ticketTypes);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a ticket type")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<TicketTypeDTO> updateTicketType(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketTypeRequest request) {
        var ticketType = ticketTypeService.updateTicketType(id, request);
        return ResponseEntity.ok(ticketType);
    }

    @PutMapping("/bulk")
    @Operation(summary = "Update multiple ticket types at once")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<List<TicketTypeDTO>> updateBulkTicketTypes(@Valid @RequestBody BulkUpdateTicketTypeRequest request) {
        var ticketTypes = ticketTypeService.updateBulkTicketTypes(request);
        return ResponseEntity.ok(ticketTypes);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a ticket type")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<Void> deleteTicketType(@PathVariable Long id) {
        ticketTypeService.deleteTicketType(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/toggle-active")
    @Operation(summary = "Enable/Disable a ticket type")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<TicketTypeDTO> toggleActiveStatus(@PathVariable Long id) {
        var ticketType = ticketTypeService.toggleActiveStatus(id);
        return ResponseEntity.ok(ticketType);
    }

    @GetMapping("/event/{eventId}/stats")
    @Operation(summary = "Get ticket type statistics for an event")
    @RequireOrganizationPermission(permission = "STATS_VIEW")
    public ResponseEntity<TicketTypeStatsDTO> getEventTicketTypeStats(@PathVariable Long eventId) {
        var stats = ticketTypeService.getEventTicketTypeStats(eventId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{id}/availability")
    @Operation(summary = "Check ticket type availability")
    public ResponseEntity<Boolean> checkAvailability(
            @PathVariable Long id,
            @Parameter(description = "Requested quantity") @RequestParam Integer quantity) {
        var isAvailable = ticketTypeService.isTicketTypeAvailable(id, quantity);
        return ResponseEntity.ok(isAvailable);
    }

    @PutMapping("/event/{eventId}/reorder")
    @Operation(summary = "Reorder ticket types")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<List<TicketTypeDTO>> reorderTicketTypes(
            @PathVariable Long eventId,
            @RequestBody List<Long> ticketTypeIds) {
        var ticketTypes = ticketTypeService.reorderTicketTypes(eventId, ticketTypeIds);
        return ResponseEntity.ok(ticketTypes);
    }

    // Overview

    @GetMapping("/event/{eventId}/overview")
    @Operation(
            summary = "Get ticket overview",
            description = "Retrieves a complete overview of all tickets and categories for an event"
    )
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<Map<String, Object>> getTicketOverview(
            @Parameter(description = "Event ID", required = true) @PathVariable Long eventId) {

        var categories = ticketCategoryService.getTicketCategoriesByEventId(eventId);
        var uncategorizedTickets = ticketTypeService.getUncategorizedTicketTypes(eventId);
        var groupedTickets = ticketTypeManagementService.getTicketTypesGroupedByCategory(eventId);

        var overview = Map.of(
                "categories", categories,
                "uncategorizedTickets", uncategorizedTickets,
                "groupedTickets", groupedTickets,
                "totalCategories", categories.size(),
                "totalUncategorized", uncategorizedTickets.size()
        );

        return ResponseEntity.ok(overview);
    }

    @GetMapping("/event/{eventId}/public")
    @Operation(
            summary = "Get public ticket information",
            description = "Retrieves ticket information visible to the public (active categories and on-sale tickets)"
    )
    public ResponseEntity<Map<String, List<TicketTypeDTO>>> getPublicTickets(
            @Parameter(description = "Event ID", required = true) @PathVariable Long eventId) {
        var publicTickets = ticketTypeManagementService.getOnSaleTicketTypesGroupedByCategory(eventId);
        return ResponseEntity.ok(publicTickets);
    }

    // Category Management

    @PostMapping("/event/{eventId}/categories")
    @Operation(summary = "Create ticket category", description = "Creates a new ticket category for the event")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Category created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "409", description = "Category name already exists")
    })
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<TicketCategoryDTO> createCategory(
            @Parameter(description = "Event ID", required = true) @PathVariable Long eventId,
            @Valid @RequestBody CreateTicketCategoryRequest request) {
        var category = ticketCategoryService.createTicketCategory(eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }

    @GetMapping("/event/{eventId}/categories")
    @Operation(summary = "Get all categories", description = "Retrieves all ticket categories for the event")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<List<TicketCategoryDTO>> getCategories(
            @Parameter(description = "Event ID", required = true) @PathVariable Long eventId) {
        var categories = ticketCategoryService.getTicketCategoriesByEventId(eventId);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/event/{eventId}/categories/active")
    @Operation(summary = "Get active categories", description = "Retrieves only active ticket categories for the event")
    public ResponseEntity<List<TicketCategoryDTO>> getActiveCategories(
            @Parameter(description = "Event ID", required = true) @PathVariable Long eventId) {
        var categories = ticketCategoryService.getActiveTicketCategoriesByEventId(eventId);
        return ResponseEntity.ok(categories);
    }

    @PutMapping("/event/{eventId}/categories/reorder")
    @Operation(summary = "Reorder categories", description = "Changes the display order of ticket categories")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<List<TicketCategoryDTO>> reorderCategories(
            @Parameter(description = "Event ID", required = true) @PathVariable Long eventId,
            @RequestBody List<Long> categoryIds) {
        var categories = ticketCategoryService.reorderTicketCategories(eventId, categoryIds);
        return ResponseEntity.ok(categories);
    }

    @DeleteMapping("/event/{eventId}/categories/{categoryId}")
    @Operation(summary = "Delete category", description = "Deletes a ticket category and moves its tickets to uncategorized")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<Void> deleteCategory(
            @Parameter(description = "Event ID", required = true) @PathVariable Long eventId,
            @Parameter(description = "Category ID", required = true) @PathVariable Long categoryId) {
        ticketCategoryService.deleteTicketCategory(categoryId);
        return ResponseEntity.noContent().build();
    }

}
