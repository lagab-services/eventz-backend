package com.lagab.eventz.app.domain.org.dto;

import com.lagab.eventz.app.domain.org.model.OrganizationRole;

public record InvitationCreateDto(
        String email,
        String organizationId,
        OrganizationRole role
) {
}
