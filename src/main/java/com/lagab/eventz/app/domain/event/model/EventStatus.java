package com.lagab.eventz.app.domain.event.model;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum EventStatus {
    DRAFT, PUBLISHED, CANCELLED, COMPLETED;

    @JsonCreator
    public static EventStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String normalizedValue = value.trim().toUpperCase();

        try {
            return EventStatus.valueOf(normalizedValue);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    String.format("Invalid EventStatus value: '%s'. Valid values are: %s (case insensitive)",
                            value,
                            Arrays.stream(EventStatus.values())
                                  .map(Enum::name)
                                  .collect(Collectors.joining(", "))
                    )
            );
        }

    }
}
