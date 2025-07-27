package com.lagab.eventz.app.interfaces.web.org.dto;

/**
 * Slug availability check response
 */
public record SlugAvailabilityResponseDto(
        String slug,
        boolean available,
        String message
) {
}
