package com.lagab.eventz.app.interfaces.web.event;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

import com.lagab.eventz.app.common.dto.PageResponse;
import com.lagab.eventz.app.domain.promotion.dto.DiscountDto;
import com.lagab.eventz.app.domain.promotion.dto.DiscountResponse;
import com.lagab.eventz.app.domain.promotion.service.DiscountService;
import com.lagab.eventz.app.interfaces.web.org.annotation.RequireOrganizationPermission;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/discounts")
@RequiredArgsConstructor
@Tag(name = "Discounts", description = "Discount management API for events")
public class DiscountController {

    private final DiscountService discountService;

    @PostMapping("/event/{eventId}")
    @Operation(summary = "Create a new discount for an event")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<DiscountResponse> createDiscount(
            @PathVariable Long eventId,
            @Valid @RequestBody DiscountDto request) {
        // Enforce eventId from the path
        DiscountDto withEvent = new DiscountDto(
                request.type(),
                request.code(),
                request.amount_off(),
                request.percent_off(),
                eventId,
                request.ticket_type_ids(),
                request.quantity_available(),
                request.start_date(),
                request.end_date(),
                request.end_date_relative(),
                request.ticket_category_id()
        );
        DiscountResponse created = discountService.createDiscount(withEvent);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get discount by ID")
    public ResponseEntity<DiscountResponse> getDiscountById(@PathVariable String id) {
        return ResponseEntity.ok(discountService.findDiscount(id));
    }

    @GetMapping("/event/{eventId}")
    @Operation(summary = "List all discounts for an event")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<PageResponse<DiscountResponse>> getDiscountsByEvent(
            @PathVariable Long eventId,
            @PageableDefault Pageable pageable) {
        var discounts = discountService.findDiscountsByEventId(eventId, pageable);
        return ResponseEntity.ok(PageResponse.of(discounts));
    }

    @PutMapping("/event/{eventId}/{id}")
    @Operation(summary = "Update a discount")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<DiscountResponse> updateDiscount(
            @Parameter(description = "Event ID", required = true) @PathVariable Long eventId,
            @PathVariable String id,
            @Valid @RequestBody DiscountDto request) {
        DiscountResponse updated = discountService.updateDiscount(id, eventId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/event/{eventId}/{id}")
    @Operation(summary = "Delete a discount")
    @RequireOrganizationPermission(permission = "EVENT_EDIT")
    public ResponseEntity<Void> deleteDiscount(
            @Parameter(description = "Event ID", required = true) @PathVariable Long eventId,
            @PathVariable String id) {
        discountService.deleteDiscount(id, eventId);
        return ResponseEntity.noContent().build();
    }
}
