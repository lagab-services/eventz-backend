package com.lagab.eventz.app.domain.org.dto.invitation;

import com.lagab.eventz.app.domain.org.model.OrganizationRole;

public record InvitationCreateDto(
        String email,
        String organizationId,
        OrganizationRole role
) {
}
