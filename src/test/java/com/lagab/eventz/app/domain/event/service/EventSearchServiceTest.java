package com.lagab.eventz.app.domain.event.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.lagab.eventz.app.domain.event.dto.EventSearchDTO;
import com.lagab.eventz.app.domain.event.dto.EventSummaryDTO;
import com.lagab.eventz.app.domain.event.mapper.EventMapper;
import com.lagab.eventz.app.domain.event.model.Event;
import com.lagab.eventz.app.domain.event.model.EventStatus;
import com.lagab.eventz.app.domain.event.model.EventType;
import com.lagab.eventz.app.domain.event.repository.EventRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventSearchServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventMapper eventMapper;

    @InjectMocks
    private EventSearchService eventSearchService;

    //@Mock
    //private ReviewRepository reviewRepository;

    @Test
    void testSearchEvents() {
        EventSearchDTO searchDTO = new EventSearchDTO();
        Pageable pageable = PageRequest.of(0, 10);
        Event event = new Event();
        EventSummaryDTO dto = getSampleDTO();

        when(eventRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of(event)));
        when(eventMapper.toSummaryDto(event)).thenReturn(dto);
        //when(reviewRepository.calculateAverageRating(dto.id())).thenReturn(4.5);
        //when(reviewRepository.countByEventId(dto.id())).thenReturn(10L);
        when(eventRepository.findById(dto.id())).thenReturn(Optional.of(event));

        Page<EventSummaryDTO> result = eventSearchService.searchEvents(searchDTO, pageable);

        assertThat(result).hasSize(1);
        verify(eventRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void testFindUpcomingFreeEvents() {
        Pageable pageable = PageRequest.of(0, 10);
        Event event = new Event();
        EventSummaryDTO dto = getSampleDTO();

        when(eventRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(event)));
        when(eventMapper.toSummaryDto(event)).thenReturn(dto);
        //when(reviewRepository.calculateAverageRating(dto.id())).thenReturn(3.8);
        //when(reviewRepository.countByEventId(dto.id())).thenReturn(5L);
        when(eventRepository.findById(dto.id())).thenReturn(Optional.of(event));

        Page<EventSummaryDTO> result = eventSearchService.findUpcomingFreeEvents(pageable);

        assertThat(result).hasSize(1);
        verify(eventRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void testFindEventsWithAvailableTickets() {
        Pageable pageable = PageRequest.of(0, 10);
        Event event = new Event();
        EventSummaryDTO dto = getSampleDTO();

        when(eventRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(event)));
        when(eventMapper.toSummaryDto(event)).thenReturn(dto);
        //when(reviewRepository.calculateAverageRating(dto.id())).thenReturn(4.2);
        //when(reviewRepository.countByEventId(dto.id())).thenReturn(8L);
        when(eventRepository.findById(dto.id())).thenReturn(Optional.of(event));

        Page<EventSummaryDTO> result = eventSearchService.findEventsWithAvailableTickets(pageable);

        assertThat(result).hasSize(1);
    }

    @Test
    void testEnrichEventSummaryDTO_shouldReturnDTOWithNullEvent() {
        Long eventId = 99L;
        EventSummaryDTO dto = getSampleDTO(eventId);

        //when(reviewRepository.calculateAverageRating(eventId)).thenReturn(2.5);
        //when(reviewRepository.countByEventId(eventId)).thenReturn(3L);
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        Page<Event> eventPage = new PageImpl<>(List.of(new Event()));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(eventPage);
        when(eventMapper.toSummaryDto(any())).thenReturn(dto);

        Page<EventSummaryDTO> result = eventSearchService.searchEvents(new EventSearchDTO(), PageRequest.of(0, 10));

        EventSummaryDTO enriched = result.getContent().get(0);
        assertThat(enriched.availableTickets()).isZero();
    }

    // Utility
    private EventSummaryDTO getSampleDTO() {
        return getSampleDTO(1L);
    }

    private EventSummaryDTO getSampleDTO(Long id) {
        return new EventSummaryDTO(
                id, "Event Name", "Summary", LocalDateTime.now(),
                LocalDateTime.now().plusDays(1), EventStatus.PUBLISHED, EventType.CONCERT, "img.jpg",
                true, true, "EUR", "Paris", "France",
                null, null, null
        );
    }
}
