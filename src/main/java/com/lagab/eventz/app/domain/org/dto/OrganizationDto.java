package com.lagab.eventz.app.domain.org.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrganizationDto(
        String id,
        String name,
        String slug,
        String email,
        String logo,
        Map<String, Object> metadata
) {
}
