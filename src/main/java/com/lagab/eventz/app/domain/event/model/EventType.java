package com.lagab.eventz.app.domain.event.model;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum EventType {
    CONFERENCE, WORKSHOP, SEMINAR, CONCERT, FESTIVAL, EXHIBITION, NETWORKING, WEBINAR, SPORT, OTHER;

    @JsonCreator
    public static EventType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String normalizedValue = value.trim().toUpperCase();

        try {
            return EventType.valueOf(normalizedValue);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    String.format("Invalid EventType value: '%s'. Valid values are: %s (case insensitive)",
                            value,
                            Arrays.stream(EventType.values())
                                  .map(Enum::name)
                                  .collect(Collectors.joining(", "))
                    )
            );
        }
    }
}
