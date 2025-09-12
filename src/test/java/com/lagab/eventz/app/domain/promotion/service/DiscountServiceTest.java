package com.lagab.eventz.app.domain.promotion.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
import com.lagab.eventz.app.domain.promotion.model.DiscountType;
import com.lagab.eventz.app.domain.promotion.repository.DiscountRepository;

import jakarta.persistence.EntityNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.verifyNoInteractions;
import static org.mockito.BDDMockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiscountService Tests")
class DiscountServiceTest {

    @Mock
    private DiscountRepository discountRepository;
    @Mock
    private TicketTypeRepository ticketTypeRepository;
    @Mock
    private DiscountMapper discountMapper;
    @Mock
    private TicketCategoryService ticketCategoryService;
    @Mock
    private EventService eventService;

    @InjectMocks
    private DiscountService discountService;

    @Nested
    @DisplayName("findByCode Tests")
    class FindByCodeTests {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should return empty Optional when code is null or empty")
        void shouldReturnEmptyWhenCodeIsNullOrEmpty(String code) {
            // When
            Optional<Discount> result = discountService.findByCode(code);

            // Then
            assertThat(result).isEmpty();
            verifyNoInteractions(discountRepository);
        }

        @Test
        @DisplayName("Should return discount when valid code exists")
        void shouldReturnDiscountWhenValidCodeExists() {
            // Given
            String code = "PROMO10";
            Discount expectedDiscount = createDiscount("1", code);
            given(discountRepository.findByCodeIgnoreCase(code))
                    .willReturn(Optional.of(expectedDiscount));

            // When
            Optional<Discount> result = discountService.findByCode(code);

            // Then
            assertThat(result)
                    .isPresent()
                    .contains(expectedDiscount);
            verify(discountRepository).findByCodeIgnoreCase(code);
        }

        @Test
        @DisplayName("Should return empty when code does not exist")
        void shouldReturnEmptyWhenCodeDoesNotExist() {
            // Given
            String code = "INVALID";
            given(discountRepository.findByCodeIgnoreCase(code))
                    .willReturn(Optional.empty());

            // When
            Optional<Discount> result = discountService.findByCode(code);

            // Then
            assertThat(result).isEmpty();
            verify(discountRepository).findByCodeIgnoreCase(code);
        }
    }

    @Nested
    @DisplayName("createDiscount Tests")
    class CreateDiscountTests {

        @Test
        @DisplayName("Should create discount with minimal data")
        void shouldCreateDiscountWithMinimalData() {
            // Given
            DiscountDto request = new DiscountDto(
                    DiscountType.PERCENTAGE, "PROMO10", null, BigDecimal.valueOf(10),
                    null, null, null, null, null, null, null
            );
            Discount entity = createDiscount("1", "PROMO10");
            Discount savedEntity = createDiscount("1", "PROMO10");
            DiscountResponse expectedResponse = createDiscountResponse("1", "PROMO10");

            given(discountMapper.toEntity(request)).willReturn(entity);
            given(discountRepository.save(entity)).willReturn(savedEntity);
            given(discountMapper.toResponse(savedEntity)).willReturn(expectedResponse);

            // When
            DiscountResponse result = discountService.createDiscount(request);

            // Then
            assertThat(result).isEqualTo(expectedResponse);
            verify(discountMapper).toEntity(request);
            verify(discountRepository).save(entity);
            verify(discountMapper).toResponse(savedEntity);
            verifyNoInteractions(eventService, ticketCategoryService, ticketTypeRepository);
        }

        @Test
        @DisplayName("Should create discount with event association")
        void shouldCreateDiscountWithEventAssociation() {
            // Given
            Long eventId = 1L;
            DiscountDto request = new DiscountDto(
                    DiscountType.PERCENTAGE, "PROMO10", null, BigDecimal.valueOf(10),
                    eventId, null, null, null, null, null, null
            );
            Discount entity = createDiscount("1", "PROMO10");
            Event event = createEvent(eventId);
            Discount savedEntity = createDiscount("1", "PROMO10");
            DiscountResponse expectedResponse = createDiscountResponse("1", "PROMO10");

            given(discountMapper.toEntity(request)).willReturn(entity);
            given(eventService.findEventById(eventId)).willReturn(event);
            given(discountRepository.save(entity)).willReturn(savedEntity);
            given(discountMapper.toResponse(savedEntity)).willReturn(expectedResponse);

            // When
            DiscountResponse result = discountService.createDiscount(request);

            // Then
            assertThat(result).isEqualTo(expectedResponse);
            verify(eventService).findEventById(eventId);
        }

        @Test
        @DisplayName("Should create discount with ticket category association")
        void shouldCreateDiscountWithTicketCategoryAssociation() {
            // Given
            Long categoryId = 1L;
            DiscountDto request = new DiscountDto(
                    DiscountType.FIXED_AMOUNT, "FIX5", BigDecimal.valueOf(5), null,
                    null, null, null, null, null, null, categoryId
            );
            Discount entity = createDiscount("1", "FIX5");
            TicketCategory category = createTicketCategory(categoryId);
            Discount savedEntity = createDiscount("1", "FIX5");
            DiscountResponse expectedResponse = createDiscountResponse("1", "FIX5");

            given(discountMapper.toEntity(request)).willReturn(entity);
            given(ticketCategoryService.getTicketCategory(categoryId)).willReturn(category);
            given(discountRepository.save(entity)).willReturn(savedEntity);
            given(discountMapper.toResponse(savedEntity)).willReturn(expectedResponse);

            // When
            DiscountResponse result = discountService.createDiscount(request);

            // Then
            assertThat(result).isEqualTo(expectedResponse);
            verify(ticketCategoryService).getTicketCategory(categoryId);
        }

        @Test
        @DisplayName("Should create discount with ticket types association")
        void shouldCreateDiscountWithTicketTypesAssociation() {
            // Given
            List<Long> typeIds = List.of(1L, 2L);
            DiscountDto request = new DiscountDto(
                    DiscountType.PERCENTAGE, "PROMO20", null, BigDecimal.valueOf(20),
                    null, typeIds, 100, null, null, null, null
            );
            Discount entity = createDiscount("1", "PROMO20");
            List<TicketType> ticketTypes = List.of(
                    createTicketType(1L),
                    createTicketType(2L)
            );
            Discount savedEntity = createDiscount("1", "PROMO20");
            DiscountResponse expectedResponse = createDiscountResponse("1", "PROMO20");

            given(discountMapper.toEntity(request)).willReturn(entity);
            given(ticketTypeRepository.findAllById(typeIds)).willReturn(ticketTypes);
            given(discountRepository.save(entity)).willReturn(savedEntity);
            given(discountMapper.toResponse(savedEntity)).willReturn(expectedResponse);

            // When
            DiscountResponse result = discountService.createDiscount(request);

            // Then
            assertThat(result).isEqualTo(expectedResponse);
            verify(ticketTypeRepository).findAllById(typeIds);
        }

        @Test
        @DisplayName("Should handle empty ticket type ids list")
        void shouldHandleEmptyTicketTypeIdsList() {
            // Given
            List<Long> emptyTypeIds = List.of();
            DiscountDto request = new DiscountDto(
                    DiscountType.PERCENTAGE, "PROMO10", null, BigDecimal.valueOf(10),
                    null, emptyTypeIds, null, null, null, null, null
            );
            Discount entity = createDiscount("1", "PROMO10");
            Discount savedEntity = createDiscount("1", "PROMO10");
            DiscountResponse expectedResponse = createDiscountResponse("1", "PROMO10");

            given(discountMapper.toEntity(request)).willReturn(entity);
            given(discountRepository.save(entity)).willReturn(savedEntity);
            given(discountMapper.toResponse(savedEntity)).willReturn(expectedResponse);

            // When
            DiscountResponse result = discountService.createDiscount(request);

            // Then
            assertThat(result).isEqualTo(expectedResponse);
            verifyNoInteractions(ticketTypeRepository);
        }
    }

    @Nested
    @DisplayName("findDiscount Tests")
    class FindDiscountTests {

        @Test
        @DisplayName("Should return discount when found")
        void shouldReturnDiscountWhenFound() {
            // Given
            String discountId = "discount-1";
            Discount discount = createDiscount(discountId, "PROMO10");
            DiscountResponse expectedResponse = createDiscountResponse(discountId, "PROMO10");

            given(discountRepository.findById(discountId)).willReturn(Optional.of(discount));
            given(discountMapper.toResponse(discount)).willReturn(expectedResponse);

            // When
            DiscountResponse result = discountService.findDiscount(discountId);

            // Then
            assertThat(result).isEqualTo(expectedResponse);
            verify(discountRepository).findById(discountId);
            verify(discountMapper).toResponse(discount);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when discount not found")
        void shouldThrowEntityNotFoundExceptionWhenDiscountNotFound() {
            // Given
            String discountId = "non-existent";
            given(discountRepository.findById(discountId)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> discountService.findDiscount(discountId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Discount not found");

            verify(discountRepository).findById(discountId);
            verifyNoInteractions(discountMapper);
        }
    }

    @Nested
    @DisplayName("findDiscountsByEventId Tests")
    class FindDiscountsByEventIdTests {

        @Test
        @DisplayName("Should return paginated discounts for event")
        void shouldReturnPaginatedDiscountsForEvent() {
            // Given
            Long eventId = 1L;
            Pageable pageable = PageRequest.of(0, 10);
            List<Discount> discounts = List.of(
                    createDiscount("1", "PROMO10"),
                    createDiscount("2", "PROMO20")
            );
            Page<Discount> discountPage = new PageImpl<>(discounts, pageable, discounts.size());

            given(discountRepository.findByEventId(eventId, pageable)).willReturn(discountPage);
            given(discountMapper.toResponse(any(Discount.class)))
                    .willReturn(createDiscountResponse("1", "PROMO10"))
                    .willReturn(createDiscountResponse("2", "PROMO20"));

            // When
            Page<DiscountResponse> result = discountService.findDiscountsByEventId(eventId, pageable);

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
            verify(discountRepository).findByEventId(eventId, pageable);
            verify(discountMapper, times(2)).toResponse(any(Discount.class));
        }

        @Test
        @DisplayName("Should return empty page when no discounts found for event")
        void shouldReturnEmptyPageWhenNoDiscountsFoundForEvent() {
            // Given
            Long eventId = 999L;
            Pageable pageable = PageRequest.of(0, 10);
            Page<Discount> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            given(discountRepository.findByEventId(eventId, pageable)).willReturn(emptyPage);

            // When
            Page<DiscountResponse> result = discountService.findDiscountsByEventId(eventId, pageable);

            // Then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            verify(discountRepository).findByEventId(eventId, pageable);
            verifyNoInteractions(discountMapper);
        }
    }

    @Nested
    @DisplayName("updateDiscount Tests")
    class UpdateDiscountTests {

        @Test
        @DisplayName("Should update discount successfully")
        void shouldUpdateDiscountSuccessfully() {
            // Given
            String discountId = "discount-1";
            Long eventId = 1L;
            Long categoryId = 2L;
            List<Long> typeIds = List.of(1L, 2L);
            DiscountDto request = new DiscountDto(
                    DiscountType.PERCENTAGE, "UPDATED", null, BigDecimal.valueOf(15),
                    eventId,
                    typeIds, 50, null, null, null, categoryId
            );

            Discount existingDiscount = createDiscount(discountId, "PROMO10");
            TicketCategory category = createTicketCategory(categoryId);
            List<TicketType> ticketTypes = List.of(createTicketType(1L), createTicketType(2L));
            Discount savedDiscount = createDiscount(discountId, "UPDATED");
            DiscountResponse expectedResponse = createDiscountResponse(discountId, "UPDATED");

            given(discountRepository.findByIdAndEventId(discountId, eventId))
                    .willReturn(Optional.of(existingDiscount));
            given(ticketCategoryService.getTicketCategory(categoryId)).willReturn(category);
            given(ticketTypeRepository.findAllById(typeIds)).willReturn(ticketTypes);
            given(discountRepository.save(existingDiscount)).willReturn(savedDiscount);
            given(discountMapper.toResponse(savedDiscount)).willReturn(expectedResponse);

            // When
            DiscountResponse result = discountService.updateDiscount(discountId, eventId, request);

            // Then
            assertThat(result).isEqualTo(expectedResponse);
            verify(discountMapper).updateEntity(request, existingDiscount);
            verify(discountRepository).save(existingDiscount);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when discount not found for update")
        void shouldThrowEntityNotFoundExceptionWhenDiscountNotFoundForUpdate() {
            // Given
            String discountId = "non-existent";
            Long eventId = 1L;
            DiscountDto request = new DiscountDto(
                    DiscountType.PERCENTAGE, "TEST", null, BigDecimal.valueOf(10),
                    eventId, null, null, null, null, null, null
            );

            given(discountRepository.findByIdAndEventId(discountId, eventId))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> discountService.updateDiscount(discountId, eventId, request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Discount not found");

            verify(discountRepository).findByIdAndEventId(discountId, eventId);
            verifyNoMoreInteractions(discountRepository);
        }
    }

    @Nested
    @DisplayName("deleteDiscount Tests")
    class DeleteDiscountTests {

        @Test
        @DisplayName("Should delete discount successfully")
        void shouldDeleteDiscountSuccessfully() {
            // Given
            String discountId = "discount-1";
            Long eventId = 1L;

            given(discountRepository.existsByIdAndEventId(discountId, eventId))
                    .willReturn(true);

            // When
            discountService.deleteDiscount(discountId, eventId);

            // Then
            verify(discountRepository).existsByIdAndEventId(discountId, eventId);
            verify(discountRepository).deleteById(discountId);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when discount not found for deletion")
        void shouldThrowResourceNotFoundExceptionWhenDiscountNotFoundForDeletion() {
            // Given
            String discountId = "non-existent";
            Long eventId = 1L;

            given(discountRepository.existsByIdAndEventId(discountId, eventId))
                    .willReturn(false);

            // When & Then
            assertThatThrownBy(() -> discountService.deleteDiscount(discountId, eventId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Discount not found");

            verify(discountRepository).existsByIdAndEventId(discountId, eventId);
            verify(discountRepository, never()).deleteById(any());
        }
    }

    // Helper methods
    private Discount createDiscount(String id, String code) {
        Discount discount = new Discount();
        discount.setId(id);
        discount.setCode(code);
        discount.setType(DiscountType.PERCENTAGE);
        discount.setPercentOff(BigDecimal.valueOf(10));
        return discount;
    }

    private DiscountResponse createDiscountResponse(String id, String code) {
        return new DiscountResponse(
                id, DiscountType.PERCENTAGE, code, null, BigDecimal.valueOf(10),
                null, null, null, 0, null, null, null, null
        );
    }

    private Event createEvent(Long id) {
        Event event = new Event();
        event.setId(id);
        return event;
    }

    private TicketCategory createTicketCategory(Long id) {
        TicketCategory category = new TicketCategory();
        category.setId(id);
        return category;
    }

    private TicketType createTicketType(Long id) {
        TicketType ticketType = new TicketType();
        ticketType.setId(id);
        return ticketType;
    }
}
