package com.lagab.eventz.app.interfaces.web.event;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lagab.eventz.app.domain.event.dto.ticket.TicketTypeDTO;
import com.lagab.eventz.app.domain.event.dto.ticket.category.CreateTicketCategoryRequest;
import com.lagab.eventz.app.domain.event.dto.ticket.category.TicketCategoryDTO;
import com.lagab.eventz.app.domain.event.dto.ticket.category.UpdateTicketCategoryRequest;
import com.lagab.eventz.app.domain.event.service.TicketCategoryService;
import com.lagab.eventz.app.domain.event.service.TicketTypeService;
import com.lagab.eventz.app.interfaces.web.org.annotation.RequireOrganizationPermission;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/ticket-categories")
@RequiredArgsConstructor
@Tag(name = "Ticket Categories", description = "Ticket category management API")
public class TicketCategoryController {

    private final TicketCategoryService ticketCategoryService;
    private final TicketTypeService ticketTypeService;

    @PostMapping("/event/{eventId}")
    @Operation(summary = "Create a new ticket category")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<TicketCategoryDTO> createTicketCategory(
            @PathVariable Long eventId,
            @Valid @RequestBody CreateTicketCategoryRequest request) {
        var ticketCategory = ticketCategoryService.createTicketCategory(eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketCategory);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a ticket category by ID")
    public ResponseEntity<TicketCategoryDTO> getTicketCategoryById(@PathVariable Long id) {
        var ticketCategory = ticketCategoryService.getTicketCategoryById(id);
        return ResponseEntity.ok(ticketCategory);
    }

    @GetMapping("/event/{eventId}")
    @Operation(summary = "Get all ticket categories for an event")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<List<TicketCategoryDTO>> getTicketCategoriesByEventId(@PathVariable Long eventId) {
        var ticketCategories = ticketCategoryService.getTicketCategoriesByEventId(eventId);
        return ResponseEntity.ok(ticketCategories);
    }

    @GetMapping("/event/{eventId}/active")
    @Operation(summary = "Get active ticket categories for an event")
    public ResponseEntity<List<TicketCategoryDTO>> getActiveTicketCategoriesByEventId(@PathVariable Long eventId) {
        var ticketCategories = ticketCategoryService.getActiveTicketCategoriesByEventId(eventId);
        return ResponseEntity.ok(ticketCategories);
    }

    @PutMapping("/event/{eventId}/{id}")
    @Operation(summary = "Update a ticket category")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<TicketCategoryDTO> updateTicketCategory(
            @PathVariable Long eventId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketCategoryRequest request) {
        var ticketCategory = ticketCategoryService.updateTicketCategory(id, request);
        return ResponseEntity.ok(ticketCategory);
    }

    @DeleteMapping("/event/{eventId}/{id}")
    @Operation(summary = "Delete a ticket category")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<Void> deleteTicketCategory(@PathVariable Long eventId, @PathVariable Long id) {
        ticketCategoryService.deleteTicketCategory(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/event/{eventId}/{id}/toggle-active")
    @Operation(summary = "Enable/Disable a ticket category")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<TicketCategoryDTO> toggleActiveStatus(
            @PathVariable Long eventId,
            @PathVariable Long id) {
        var ticketCategory = ticketCategoryService.toggleActiveStatus(id);
        return ResponseEntity.ok(ticketCategory);
    }

    @PostMapping("/event/{eventId}/{id}/toggle-collapse")
    @Operation(summary = "Collapse/Expand a ticket category")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<TicketCategoryDTO> toggleCollapseStatus(
            @PathVariable Long eventId,
            @PathVariable Long id) {
        var ticketCategory = ticketCategoryService.toggleCollapseStatus(id);
        return ResponseEntity.ok(ticketCategory);
    }

    @PutMapping("/event/{eventId}/reorder")
    @Operation(summary = "Reorder ticket categories")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<List<TicketCategoryDTO>> reorderTicketCategories(
            @PathVariable Long eventId,
            @RequestBody List<Long> categoryIds) {
        var ticketCategories = ticketCategoryService.reorderTicketCategories(eventId, categoryIds);
        return ResponseEntity.ok(ticketCategories);
    }

    @PutMapping("/event/{eventId}/{categoryId}/move-ticket-types")
    @Operation(summary = "Move ticket types to a category")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<List<TicketTypeDTO>> moveTicketTypesToCategory(
            @PathVariable Long eventId,
            @PathVariable Long categoryId,
            @RequestBody List<Long> ticketTypeIds) {
        var ticketTypes = ticketTypeService.moveTicketTypesToCategory(ticketTypeIds, categoryId);
        return ResponseEntity.ok(ticketTypes);
    }

}
