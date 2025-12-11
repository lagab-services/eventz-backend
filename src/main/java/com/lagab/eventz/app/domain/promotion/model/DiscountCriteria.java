package com.lagab.eventz.app.domain.promotion.model;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.lagab.eventz.app.domain.cart.model.CartItem;
import com.lagab.eventz.app.domain.event.model.Event;
import com.lagab.eventz.app.domain.event.model.TicketCategory;
import com.lagab.eventz.app.domain.event.model.TicketType;
import com.lagab.eventz.app.domain.event.repository.TicketTypeRepository;

public record DiscountCriteria(
        Optional<Long> requiredEventId,
        Optional<Set<Long>> allowedTypeIds,
        Optional<Long> requiredCategoryId
) {

    public static DiscountCriteria from(Discount discount) {
        return new DiscountCriteria(
                extractEventId(discount),
                extractAllowedTypeIds(discount),
                extractCategoryId(discount)
        );
    }

    private static Optional<Long> extractEventId(Discount discount) {
        return Optional.ofNullable(discount.getEvent())
                       .map(Event::getId);
    }

    private static Optional<Set<Long>> extractAllowedTypeIds(Discount discount) {
        return Optional.ofNullable(discount.getTicketTypes())
                       .filter(types -> !types.isEmpty())
                       .map(types -> types.stream()
                                          .map(TicketType::getId)
                                          .collect(Collectors.toUnmodifiableSet()));
    }

    private static Optional<Long> extractCategoryId(Discount discount) {
        return Optional.ofNullable(discount.getTicketCategory())
                       .map(TicketCategory::getId);
    }

    public boolean isEligible(CartItem item, TicketTypeRepository repository) {
        return isEventEligible(item)
                && isTypeEligible(item)
                && isCategoryEligible(item, repository);
    }

    private boolean isEventEligible(CartItem item) {
        return requiredEventId
                .map(eventId -> eventId.equals(item.getEventId()))
                .orElse(true);
    }

    private boolean isTypeEligible(CartItem item) {
        return allowedTypeIds
                .map(typeIds -> typeIds.contains(item.getTicketTypeId()))
                .orElse(true);
    }

    private boolean isCategoryEligible(CartItem item, TicketTypeRepository repository) {
        return requiredCategoryId
                .map(categoryId -> findTicketTypeCategory(item.getTicketTypeId(), repository)
                        .map(category -> categoryId.equals(category.getId()))
                        .orElse(false))
                .orElse(true);
    }

    private Optional<TicketCategory> findTicketTypeCategory(Long ticketTypeId, TicketTypeRepository repository) {
        return repository.findById(ticketTypeId)
                         .map(TicketType::getCategory);
    }

    public boolean hasNoRestrictions() {
        return requiredEventId.isEmpty()
                && allowedTypeIds.isEmpty()
                && requiredCategoryId.isEmpty();
    }
}
