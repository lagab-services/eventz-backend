package com.lagab.eventz.app.domain.ticket.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.lagab.eventz.app.common.exception.BusinessException;
import com.lagab.eventz.app.common.exception.ResourceNotFoundException;
import com.lagab.eventz.app.common.exception.ValidationException;
import com.lagab.eventz.app.domain.event.model.Event;
import com.lagab.eventz.app.domain.event.model.EventCustomField;
import com.lagab.eventz.app.domain.event.repository.EventCustomFieldRepository;
import com.lagab.eventz.app.domain.order.model.Order;
import com.lagab.eventz.app.domain.ticket.dto.AttendeeInfo;
import com.lagab.eventz.app.domain.ticket.dto.AttendeeResponse;
import com.lagab.eventz.app.domain.ticket.dto.AttendeeSearchCriteria;
import com.lagab.eventz.app.domain.ticket.dto.AttendeeStatistics;
import com.lagab.eventz.app.domain.ticket.dto.TransferTicketRequest;
import com.lagab.eventz.app.domain.ticket.entity.Attendee;
import com.lagab.eventz.app.domain.ticket.entity.AttendeeCustomField;
import com.lagab.eventz.app.domain.ticket.entity.CheckInStatus;
import com.lagab.eventz.app.domain.ticket.entity.Ticket;
import com.lagab.eventz.app.domain.ticket.repository.AttendeeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttendeeService {
    private final AttendeeRepository attendeeRepository;
    private final EventCustomFieldRepository eventCustomFieldRepository;

    public void createAttendee(AttendeeInfo request, Order order, Ticket ticket) {
        // Validate custom fields
        validateCustomFields(request.customFields(), order.getEvent().getId(), request.ticketTypeId());

        Attendee attendee = new Attendee();
        attendee.setFirstName(request.firstName());
        attendee.setLastName(request.lastName());
        attendee.setEmail(request.email());
        attendee.setTicket(ticket);
        attendee.setOrder(order);
        attendee.setEvent(order.getEvent());

        attendee = attendeeRepository.save(attendee);

        // Save custom fields
        if (request.customFields() != null && !request.customFields().isEmpty()) {
            saveCustomFields(attendee, request.customFields());
        }

    }

    public void checkInAttendee(Long attendeeId, String checkedInBy) {
        Attendee attendee = getAttendee(attendeeId);

        attendee.setCheckInStatus(CheckInStatus.CHECKED_IN);
        attendee.setCheckedInAt(LocalDateTime.now());
        attendee.setCheckedInBy(checkedInBy);

        attendeeRepository.save(attendee);
    }

    private Attendee getAttendee(Long attendeeId) {
        Attendee attendee = attendeeRepository.findById(attendeeId)
                                              .orElseThrow(() -> new ResourceNotFoundException("Attendee not found"));
        return attendee;
    }

    public Page<AttendeeResponse> getEventAttendees(Long eventId, Pageable pageable) {
        return attendeeRepository.findByEventId(eventId, pageable)
                                 .map(this::mapToResponse);
    }

    public AttendeeStatistics getEventStatistics(Long eventId) {
        List<Attendee> attendees = attendeeRepository.findByEventId(eventId);

        long totalAttendees = attendees.size();
        long checkedIn = attendees.stream()
                                  .mapToLong(a -> a.getCheckInStatus() == CheckInStatus.CHECKED_IN ? 1 : 0)
                                  .sum();
        long cancelled = attendees.stream()
                                  .mapToLong(a -> a.getCheckInStatus() == CheckInStatus.CANCELLED ? 1 : 0)
                                  .sum();

        Map<String, Long> attendeesByTicketType = attendees.stream()
                                                           .collect(Collectors.groupingBy(
                                                                   a -> a.getTicket().getTicketType().getName(),
                                                                   Collectors.counting()
                                                           ));

        return new AttendeeStatistics(
                totalAttendees,
                checkedIn,
                cancelled,
                attendeesByTicketType
        );
    }

    public List<Attendee> findUnassignedAttendees(Long orderId) {
        return attendeeRepository.findByOrderIdAndTicketIsNull(orderId);
    }

    public AttendeeResponse updateAttendee(Long attendeeId, AttendeeInfo request) {
        Attendee attendee = getAttendee(attendeeId);

        // Update basic information
        attendee.setFirstName(request.firstName());
        attendee.setLastName(request.lastName());
        attendee.setEmail(request.email());
        attendee.setUpdatedAt(LocalDateTime.now());

        // Update custom fields
        if (request.customFields() != null) {
            updateCustomFields(attendee, request.customFields());
        }

        attendee = attendeeRepository.save(attendee);
        return mapToResponse(attendee);
    }

    public AttendeeResponse transferTicket(Long attendeeId, TransferTicketRequest request) {
        Attendee currentAttendee = getAttendee(attendeeId);

        // Verify transfer is allowed
        if (!isTransferAllowed(currentAttendee)) {
            throw new BusinessException("Ticket transfer not allowed for this event");
        }

        // Create new attendee
        Attendee newAttendee = new Attendee();
        newAttendee.setFirstName(request.firstName());
        newAttendee.setLastName(request.lastName());
        newAttendee.setEmail(request.email());
        newAttendee.setTicket(currentAttendee.getTicket());
        newAttendee.setOrder(currentAttendee.getOrder());
        newAttendee.setEvent(currentAttendee.getEvent());

        // Delete old attendee
        attendeeRepository.delete(currentAttendee);

        // Save new attendee
        newAttendee = attendeeRepository.save(newAttendee);

        // Todo: Send notification emails
        //emailService.sendTransferNotificationEmail(currentAttendee, newAttendee);

        return mapToResponse(newAttendee);
    }

    public List<AttendeeResponse> searchAttendees(AttendeeSearchCriteria criteria) {
        return attendeeRepository.findByCriteria(criteria.eventId(), criteria.name(), criteria.email(), criteria.checkInStatus(),
                                         criteria.ticketTypeId())
                                 .stream()
                                 .map(this::mapToResponse)
                                 .toList();
    }

    /*
    Todo: generate pdf
    public byte[] generateTicketPdf(Long attendeeId) {
        Attendee attendee = attendeeRepository.findById(attendeeId)
                                              .orElseThrow(() -> new EntityNotFoundException("Attendee not found"));

        return pdfService.generateTicket(attendee);
    }*/

    private boolean isTransferAllowed(Attendee attendee) {
        Event event = attendee.getEvent();
        return event.getStartDate().isAfter(LocalDateTime.now().plusDays(1));
    }

    private void validateCustomFields(Map<String, String> customFields, Long eventId, Long ticketTypeId) {
        List<EventCustomField> requiredFields = eventCustomFieldRepository
                .findRequiredFieldsByEventAndTicketType(eventId, ticketTypeId);

        for (EventCustomField field : requiredFields) {
            if (customFields == null || !customFields.containsKey(field.getFieldName())
                    || customFields.get(field.getFieldName()).trim().isEmpty()) {
                throw new ValidationException("Required field missing: " + field.getFieldLabel());
            }
        }
    }

    private void saveCustomFields(Attendee attendee, Map<String, String> customFields) {
        List<AttendeeCustomField> fields = customFields.entrySet().stream()
                                                       .map(entry -> {
                                                           AttendeeCustomField field = new AttendeeCustomField();
                                                           field.setAttendee(attendee);
                                                           field.setFieldName(entry.getKey());
                                                           field.setFieldValue(entry.getValue());
                                                           return field;
                                                       })
                                                       .toList();

        attendee.setCustomFields(fields);
    }

    private void updateCustomFields(Attendee attendee, Map<String, String> customFields) {
        // Remove old fields
        attendee.getCustomFields().clear();

        // Add new fields
        List<AttendeeCustomField> newFields = customFields.entrySet().stream()
                                                          .map(entry -> {
                                                              AttendeeCustomField field = new AttendeeCustomField();
                                                              field.setAttendee(attendee);
                                                              field.setFieldName(entry.getKey());
                                                              field.setFieldValue(entry.getValue());
                                                              return field;
                                                          })
                                                          .toList();

        attendee.getCustomFields().addAll(newFields);
    }

    private AttendeeResponse mapToResponse(Attendee attendee) {
        Map<String, String> customFields = attendee.getCustomFields().stream()
                                                   .collect(Collectors.toMap(
                                                           AttendeeCustomField::getFieldName,
                                                           AttendeeCustomField::getFieldValue
                                                   ));

        return new AttendeeResponse(
                attendee.getId(),
                attendee.getFirstName(),
                attendee.getLastName(),
                attendee.getEmail(),
                attendee.getTicket().getTicketCode(),
                attendee.getTicket().getTicketType().getName(),
                attendee.getCheckInStatus(),
                customFields
        );
    }

}
