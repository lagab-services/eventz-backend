package com.lagab.eventz.app.domain.promotion.service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lagab.eventz.app.common.exception.ResourceNotFoundException;
import com.lagab.eventz.app.domain.event.model.Event;
import com.lagab.eventz.app.domain.event.model.TicketCategory;
import com.lagab.eventz.app.domain.event.model.TicketType;
import com.lagab.eventz.app.domain.event.repository.TicketTypeRepository;
import com.lagab.eventz.app.domain.event.service.EventService;
import com.lagab.eventz.app.domain.event.service.TicketCategoryService;
import com.lagab.eventz.app.domain.promotion.dto.DiscountDto;
import com.lagab.eventz.app.domain.promotion.dto.DiscountResponse;
import com.lagab.eventz.app.domain.promotion.mapper.DiscountMapper;
import com.lagab.eventz.app.domain.promotion.model.Discount;
import com.lagab.eventz.app.domain.promotion.repository.DiscountRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DiscountService {

    private final DiscountRepository discountRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final DiscountMapper discountMapper;
    private final TicketCategoryService ticketCategoryService;
    private final EventService eventService;

    public Optional<Discount> findByCode(String code) {
        if (StringUtils.isEmpty(code))
            return Optional.empty();
        return discountRepository.findByCodeIgnoreCase(code);
    }

    public DiscountResponse createDiscount(DiscountDto request) {
        Discount entity = discountMapper.toEntity(request);

        if (request.eventId() != null) {
            Event event = eventService.findEventById(request.eventId());
            entity.setEvent(event);
        }
        if (request.ticket_category_id() != null) {
            TicketCategory cat = ticketCategoryService.getTicketCategory(request.ticket_category_id());
            entity.setTicketCategory(cat);
        }
        if (request.ticket_type_ids() != null && !request.ticket_type_ids().isEmpty()) {
            Set<TicketType> types = new HashSet<>(ticketTypeRepository.findAllById(request.ticket_type_ids()));
            entity.setTicketTypes(types);
        }

        Discount saved = discountRepository.save(entity);
        return discountMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public DiscountResponse findDiscount(String id) {
        Discount entity = discountRepository.findById(id)
                                            .orElseThrow(() -> new EntityNotFoundException("Discount not found"));
        return discountMapper.toResponse(entity);
    }

    @Transactional(readOnly = true)
    public Page<DiscountResponse> findDiscountsByEventId(Long eventId, Pageable pageable) {
        return discountRepository.findByEventId(eventId, pageable).map(discountMapper::toResponse);
    }

    @Transactional
    public DiscountResponse updateDiscount(String id, Long eventId, DiscountDto request) {
        Discount entity = discountRepository.findByIdAndEventId(id, eventId)
                                            .orElseThrow(() -> new EntityNotFoundException("Discount not found"));
        discountMapper.updateEntity(request, entity);
        if (request.ticket_category_id() != null) {
            TicketCategory cat = ticketCategoryService.getTicketCategory(request.ticket_category_id());
            entity.setTicketCategory(cat);
        }
        if (request.ticket_type_ids() != null) {
            Set<TicketType> types = new HashSet<>(ticketTypeRepository.findAllById(request.ticket_type_ids()));
            entity.setTicketTypes(types);
        }
        Discount saved = discountRepository.save(entity);
        return discountMapper.toResponse(saved);
    }

    public void deleteDiscount(String id, Long eventId) {
        if (!discountRepository.existsByIdAndEventId(id, eventId)) {
            throw new ResourceNotFoundException("Discount not found");
        }
        discountRepository.deleteById(id);
    }
}
