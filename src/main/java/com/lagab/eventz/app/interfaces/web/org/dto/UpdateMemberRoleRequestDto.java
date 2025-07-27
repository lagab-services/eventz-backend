package com.lagab.eventz.app.interfaces.web.org.dto;

import com.lagab.eventz.app.domain.org.model.OrganizationRole;

import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequestDto(
        @NotNull(message = "Role is required")
        OrganizationRole role
) {
}
