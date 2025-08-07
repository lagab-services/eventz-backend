package com.lagab.eventz.app.domain.org.dto.invitation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lagab.eventz.app.domain.org.model.OrganizationRole;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MembershipInviteDto(
        Long userId,
        String email,
        String organizationId,
        OrganizationRole role
) {
}
