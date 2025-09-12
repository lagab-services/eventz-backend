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

import com.lagab.eventz.app.domain.event.dto.customfield.EventCustomFieldDTO;
import com.lagab.eventz.app.domain.event.dto.customfield.EventCustomFieldRequest;
import com.lagab.eventz.app.domain.event.service.EventCustomFieldService;
import com.lagab.eventz.app.interfaces.web.org.annotation.RequireOrganizationPermission;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/custom-fields")
@RequiredArgsConstructor
@Tag(name = "Event Custom Fields", description = "Manage custom fields for events and ticket types")
public class EventCustomFieldController {

    private final EventCustomFieldService customFieldService;

    @PostMapping("/event/{eventId}")
    @Operation(summary = "Create a new custom field for an event")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<EventCustomFieldDTO> createCustomField(
            @Parameter(description = "Event ID") @PathVariable Long eventId,
            @Valid @RequestBody EventCustomFieldRequest request) {
        var created = customFieldService.createCustomField(eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/event/{eventId}")
    @Operation(summary = "List custom fields for an event (ordered by display order)")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<List<EventCustomFieldDTO>> getEventCustomFields(
            @Parameter(description = "Event ID") @PathVariable Long eventId) {
        var fields = customFieldService.getEventCustomFields(eventId);
        return ResponseEntity.ok(fields);
    }

    @GetMapping("/event/{eventId}/ticket-type/{ticketTypeId}")
    @Operation(summary = "List custom fields for a specific ticket type within an event")
    public ResponseEntity<List<EventCustomFieldDTO>> getCustomFieldsForTicketType(
            @Parameter(description = "Event ID") @PathVariable Long eventId,
            @Parameter(description = "Ticket Type ID") @PathVariable Long ticketTypeId) {
        var fields = customFieldService.getCustomFieldsForTicketType(eventId, ticketTypeId);
        return ResponseEntity.ok(fields);
    }

    @PutMapping("/{fieldId}")
    @Operation(summary = "Update an existing custom field")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<EventCustomFieldDTO> updateCustomField(
            @Parameter(description = "Custom Field ID") @PathVariable Long fieldId,
            @Valid @RequestBody EventCustomFieldRequest request) {
        var updated = customFieldService.updateCustomField(fieldId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{fieldId}")
    @Operation(summary = "Delete a custom field")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<Void> deleteCustomField(
            @Parameter(description = "Custom Field ID") @PathVariable Long fieldId) {
        customFieldService.deleteCustomField(fieldId);
        return ResponseEntity.noContent().build();
    }
}
