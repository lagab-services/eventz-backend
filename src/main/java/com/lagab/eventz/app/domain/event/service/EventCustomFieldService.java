package com.lagab.eventz.app.domain.event.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.lagab.eventz.app.common.exception.ResourceNotFoundException;
import com.lagab.eventz.app.domain.event.dto.customfield.EventCustomFieldDTO;
import com.lagab.eventz.app.domain.event.dto.customfield.EventCustomFieldRequest;
import com.lagab.eventz.app.domain.event.mapper.EventCustomFieldMapper;
import com.lagab.eventz.app.domain.event.model.Event;
import com.lagab.eventz.app.domain.event.model.EventCustomField;
import com.lagab.eventz.app.domain.event.model.TicketType;
import com.lagab.eventz.app.domain.event.repository.EventCustomFieldRepository;
import com.lagab.eventz.app.domain.event.repository.EventRepository;
import com.lagab.eventz.app.domain.event.repository.TicketTypeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventCustomFieldService {

    private final EventCustomFieldRepository customFieldRepository;
    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final EventCustomFieldMapper eventCustomFieldMapper;

    public List<EventCustomFieldDTO> getEventCustomFields(Long eventId) {
        return customFieldRepository.findByEventIdOrderByDisplayOrder(eventId)
                                    .stream()
                                    .map(eventCustomFieldMapper::toDto)
                                    .toList();
    }

    public EventCustomFieldDTO createCustomField(Long eventId, EventCustomFieldRequest request) {
        Event event = eventRepository.findById(eventId)
                                     .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        EventCustomField customField = eventCustomFieldMapper.fromRequest(request);
        customField.setEvent(event);

        if (request.ticketTypeId() != null) {
            TicketType ticketType = ticketTypeRepository.findById(request.ticketTypeId())
                                                        .orElseThrow(() -> new ResourceNotFoundException("Ticket type not found"));
            customField.setTicketType(ticketType);
        }

        customField = customFieldRepository.save(customField);
        return eventCustomFieldMapper.toDto(customField);
    }

    public EventCustomFieldDTO updateCustomField(Long fieldId, EventCustomFieldRequest request) {
        EventCustomField customField = customFieldRepository.findById(fieldId)
                                                            .orElseThrow(() -> new ResourceNotFoundException("Custom field not found"));

        eventCustomFieldMapper.updateEntityFromRequest(request, customField);

        customField = customFieldRepository.save(customField);
        return eventCustomFieldMapper.toDto(customField);
    }

    public void deleteCustomField(Long fieldId) {
        if (!customFieldRepository.existsById(fieldId)) {
            throw new ResourceNotFoundException("Custom field not found");
        }
        customFieldRepository.deleteById(fieldId);
    }

    public List<EventCustomFieldDTO> getCustomFieldsForTicketType(Long eventId, Long ticketTypeId) {
        return customFieldRepository.findByEventIdAndTicketTypeIdOrderByDisplayOrder(eventId, ticketTypeId)
                                    .stream()
                                    .map(eventCustomFieldMapper::toDto)
                                    .toList();
    }
}
