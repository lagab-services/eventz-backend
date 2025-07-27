package com.lagab.eventz.app.domain.org.dto;

public record UserInfoDto(
        Long id,
        String email,
        String firstName,
        String lastName
) {
}
