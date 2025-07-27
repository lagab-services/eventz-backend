package com.lagab.eventz.app.domain.org.dto;

import java.time.LocalDateTime;

import com.lagab.eventz.app.domain.org.model.OrganizationRole;

public record InvitationResponseDto(
        Long id,
        String email,
        OrganizationRole role,
        String token,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        UserInfoDto invitedBy,
        OrganizationDto organization
) {
}
