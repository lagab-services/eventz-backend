package com.lagab.eventz.app.domain.event.dto.customfield;

import com.lagab.eventz.app.domain.event.model.FieldType;

public record EventCustomFieldDTO(
        Long id,
        String fieldName,
        String fieldLabel,
        FieldType fieldType,
        Boolean isRequired,
        String fieldOptions,
        String placeholder,
        Integer displayOrder,
        Long ticketTypeId
) {
}
