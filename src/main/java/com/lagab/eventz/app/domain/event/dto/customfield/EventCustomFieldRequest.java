package com.lagab.eventz.app.domain.event.dto.customfield;

import com.lagab.eventz.app.domain.event.model.FieldType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EventCustomFieldRequest(
        @NotBlank String fieldName,
        @NotBlank String fieldLabel,
        @NotNull FieldType fieldType,
        Boolean isRequired,
        String fieldOptions, // JSON pour les options
        String placeholder,
        Integer displayOrder,
        Long ticketTypeId // Optionnel, si le champ est spécifique à un type de ticket
) {
}
