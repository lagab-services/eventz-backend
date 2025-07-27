package com.lagab.eventz.app.domain.org.dto;

import com.lagab.eventz.app.domain.org.model.OrganizationRole;

public record OrganizationMembershipDto(
        Long id,
        OrganizationDto organization,
        OrganizationRole role,
        UserInfoDto user
) {
}
