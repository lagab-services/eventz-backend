package com.lagab.eventz.app.domain.event.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lagab.eventz.app.common.exception.ResourceNotFoundException;
import com.lagab.eventz.app.domain.event.dto.customfield.EventCustomFieldDTO;
import com.lagab.eventz.app.domain.event.dto.customfield.EventCustomFieldRequest;
import com.lagab.eventz.app.domain.event.mapper.EventCustomFieldMapper;
import com.lagab.eventz.app.domain.event.model.Event;
import com.lagab.eventz.app.domain.event.model.EventCustomField;
import com.lagab.eventz.app.domain.event.model.FieldType;
import com.lagab.eventz.app.domain.event.model.TicketType;
import com.lagab.eventz.app.domain.event.repository.EventCustomFieldRepository;
import com.lagab.eventz.app.domain.event.repository.EventRepository;
import com.lagab.eventz.app.domain.event.repository.TicketTypeRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventCustomFieldService Tests")
class EventCustomFieldServiceTest {

    @Mock
    private EventCustomFieldRepository customFieldRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @Mock
    private EventCustomFieldMapper eventCustomFieldMapper;

    @InjectMocks
    private EventCustomFieldService eventCustomFieldService;

    private Event event;
    private TicketType ticketType;
    private EventCustomField customField;
    private EventCustomFieldRequest request;
    private EventCustomFieldDTO dto;

    @BeforeEach
    void setUp() {
        event = new Event();
        event.setId(1L);

        ticketType = new TicketType();
        ticketType.setId(1L);

        customField = new EventCustomField();
        customField.setId(1L);
        customField.setFieldName("firstName");
        customField.setFieldLabel("First Name");
        customField.setFieldType(FieldType.TEXT);
        customField.setRequired(true);
        customField.setPlaceholder("Enter your first name");
        customField.setDisplayOrder(1);
        customField.setEvent(event);

        request = new EventCustomFieldRequest(
                "firstName",
                "First Name",
                FieldType.TEXT,
                true,
                null,
                "Enter your first name",
                1,
                null
        );

        dto = new EventCustomFieldDTO(
                1L,
                "firstName",
                "First Name",
                FieldType.TEXT,
                true,
                null,
                "Enter your first name",
                1,
                null
        );
    }

    @Nested
    @DisplayName("getEventCustomFields Tests")
    class GetEventCustomFieldsTests {

        @Test
        @DisplayName("Should return list of custom fields for event")
        void shouldReturnCustomFieldsForEvent() {
            // Given
            Long eventId = 1L;
            List<EventCustomField> customFields = List.of(customField);
            when(customFieldRepository.findByEventIdOrderByDisplayOrder(eventId))
                    .thenReturn(customFields);
            when(eventCustomFieldMapper.toDto(customField)).thenReturn(dto);

            // When
            List<EventCustomFieldDTO> result = eventCustomFieldService.getEventCustomFields(eventId);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst()).isEqualTo(dto);
            verify(customFieldRepository).findByEventIdOrderByDisplayOrder(eventId);
            verify(eventCustomFieldMapper).toDto(customField);
        }

        @Test
        @DisplayName("Should return empty list when no custom fields exist")
        void shouldReturnEmptyListWhenNoCustomFields() {
            // Given
            Long eventId = 1L;
            when(customFieldRepository.findByEventIdOrderByDisplayOrder(eventId))
                    .thenReturn(List.of());

            // When
            List<EventCustomFieldDTO> result = eventCustomFieldService.getEventCustomFields(eventId);

            // Then
            assertThat(result).isEmpty();
            verify(customFieldRepository).findByEventIdOrderByDisplayOrder(eventId);
            verify(eventCustomFieldMapper, never()).toDto(any());
        }
    }

    @Nested
    @DisplayName("createCustomField Tests")
    class CreateCustomFieldTests {

        @Test
        @DisplayName("Should create custom field successfully without ticket type")
        void shouldCreateCustomFieldWithoutTicketType() {
            // Given
            Long eventId = 1L;
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(eventCustomFieldMapper.fromRequest(request)).thenReturn(customField);
            when(customFieldRepository.save(customField)).thenReturn(customField);
            when(eventCustomFieldMapper.toDto(customField)).thenReturn(dto);

            // When
            EventCustomFieldDTO result = eventCustomFieldService.createCustomField(eventId, request);

            // Then
            assertThat(result).isEqualTo(dto);
            assertThat(customField.getEvent()).isEqualTo(event);
            verify(eventRepository).findById(eventId);
            verify(eventCustomFieldMapper).fromRequest(request);
            verify(customFieldRepository).save(customField);
            verify(eventCustomFieldMapper).toDto(customField);
            verify(ticketTypeRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("Should create custom field successfully with ticket type")
        void shouldCreateCustomFieldWithTicketType() {
            // Given
            Long eventId = 1L;
            Long ticketTypeId = 1L;
            EventCustomFieldRequest requestWithTicketType = new EventCustomFieldRequest(
                    "firstName",
                    "First Name",
                    FieldType.TEXT,
                    true,
                    null,
                    "Enter your first name",
                    1,
                    ticketTypeId
            );

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(ticketTypeRepository.findById(ticketTypeId)).thenReturn(Optional.of(ticketType));
            when(eventCustomFieldMapper.fromRequest(requestWithTicketType)).thenReturn(customField);
            when(customFieldRepository.save(customField)).thenReturn(customField);
            when(eventCustomFieldMapper.toDto(customField)).thenReturn(dto);

            // When
            EventCustomFieldDTO result = eventCustomFieldService.createCustomField(eventId, requestWithTicketType);

            // Then
            assertThat(result).isEqualTo(dto);
            assertThat(customField.getEvent()).isEqualTo(event);
            assertThat(customField.getTicketType()).isEqualTo(ticketType);
            verify(eventRepository).findById(eventId);
            verify(ticketTypeRepository).findById(ticketTypeId);
            verify(eventCustomFieldMapper).fromRequest(requestWithTicketType);
            verify(customFieldRepository).save(customField);
            verify(eventCustomFieldMapper).toDto(customField);
        }

        @Test
        @DisplayName("Should throw exception when event not found")
        void shouldThrowExceptionWhenEventNotFound() {
            // Given
            Long eventId = 999L;
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> eventCustomFieldService.createCustomField(eventId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Event not found");

            verify(eventRepository).findById(eventId);
            verify(eventCustomFieldMapper, never()).fromRequest(any());
            verify(customFieldRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when ticket type not found")
        void shouldThrowExceptionWhenTicketTypeNotFound() {
            // Given
            Long eventId = 1L;
            Long ticketTypeId = 999L;
            EventCustomFieldRequest requestWithTicketType = new EventCustomFieldRequest(
                    "firstName",
                    "First Name",
                    FieldType.TEXT,
                    true,
                    null,
                    "Enter your first name",
                    1,
                    ticketTypeId
            );

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(ticketTypeRepository.findById(ticketTypeId)).thenReturn(Optional.empty());
            when(eventCustomFieldMapper.fromRequest(requestWithTicketType)).thenReturn(customField);

            // When & Then
            assertThatThrownBy(() -> eventCustomFieldService.createCustomField(eventId, requestWithTicketType))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Ticket type not found");

            verify(eventRepository).findById(eventId);
            verify(ticketTypeRepository).findById(ticketTypeId);
            verify(eventCustomFieldMapper).fromRequest(requestWithTicketType);
            verify(customFieldRepository, never()).save(any());
        }

    }

    @Nested
    @DisplayName("updateCustomField Tests")
    class UpdateCustomFieldTests {

        @Test
        @DisplayName("Should update custom field successfully")
        void shouldUpdateCustomFieldSuccessfully() {
            // Given
            Long fieldId = 1L;
            when(customFieldRepository.findById(fieldId)).thenReturn(Optional.of(customField));
            when(customFieldRepository.save(customField)).thenReturn(customField);
            when(eventCustomFieldMapper.toDto(customField)).thenReturn(dto);

            // When
            EventCustomFieldDTO result = eventCustomFieldService.updateCustomField(fieldId, request);

            // Then
            assertThat(result).isEqualTo(dto);
            verify(customFieldRepository).findById(fieldId);
            verify(eventCustomFieldMapper).updateEntityFromRequest(request, customField);
            verify(customFieldRepository).save(customField);
            verify(eventCustomFieldMapper).toDto(customField);
        }

        @Test
        @DisplayName("Should throw exception when custom field not found")
        void shouldThrowExceptionWhenCustomFieldNotFound() {
            // Given
            Long fieldId = 999L;
            when(customFieldRepository.findById(fieldId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> eventCustomFieldService.updateCustomField(fieldId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Custom field not found");

            verify(customFieldRepository).findById(fieldId);
            verify(eventCustomFieldMapper, never()).updateEntityFromRequest(any(), any());
            verify(customFieldRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteCustomField Tests")
    class DeleteCustomFieldTests {

        @Test
        @DisplayName("Should delete custom field successfully")
        void shouldDeleteCustomFieldSuccessfully() {
            // Given
            Long fieldId = 1L;
            when(customFieldRepository.existsById(fieldId)).thenReturn(true);

            // When
            eventCustomFieldService.deleteCustomField(fieldId);

            // Then
            verify(customFieldRepository).existsById(fieldId);
            verify(customFieldRepository).deleteById(fieldId);
        }

        @Test
        @DisplayName("Should throw exception when custom field not found")
        void shouldThrowExceptionWhenCustomFieldNotFoundForDelete() {
            // Given
            Long fieldId = 999L;
            when(customFieldRepository.existsById(fieldId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> eventCustomFieldService.deleteCustomField(fieldId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Custom field not found");

            verify(customFieldRepository).existsById(fieldId);
            verify(customFieldRepository, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("getCustomFieldsForTicketType Tests")
    class GetCustomFieldsForTicketTypeTests {

        @Test
        @DisplayName("Should return custom fields for specific ticket type")
        void shouldReturnCustomFieldsForTicketType() {
            // Given
            Long eventId = 1L;
            Long ticketTypeId = 1L;
            customField.setTicketType(ticketType);
            List<EventCustomField> customFields = List.of(customField);

            when(customFieldRepository.findByEventIdAndTicketTypeIdOrderByDisplayOrder(eventId, ticketTypeId))
                    .thenReturn(customFields);
            when(eventCustomFieldMapper.toDto(customField)).thenReturn(dto);

            // When
            List<EventCustomFieldDTO> result = eventCustomFieldService.getCustomFieldsForTicketType(eventId, ticketTypeId);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst()).isEqualTo(dto);
            verify(customFieldRepository).findByEventIdAndTicketTypeIdOrderByDisplayOrder(eventId, ticketTypeId);
            verify(eventCustomFieldMapper).toDto(customField);
        }

        @Test
        @DisplayName("Should return empty list when no custom fields for ticket type")
        void shouldReturnEmptyListWhenNoCustomFieldsForTicketType() {
            // Given
            Long eventId = 1L;
            Long ticketTypeId = 1L;
            when(customFieldRepository.findByEventIdAndTicketTypeIdOrderByDisplayOrder(eventId, ticketTypeId))
                    .thenReturn(List.of());

            // When
            List<EventCustomFieldDTO> result = eventCustomFieldService.getCustomFieldsForTicketType(eventId, ticketTypeId);

            // Then
            assertThat(result).isEmpty();
            verify(customFieldRepository).findByEventIdAndTicketTypeIdOrderByDisplayOrder(eventId, ticketTypeId);
            verify(eventCustomFieldMapper, never()).toDto(any());
        }
    }
}
