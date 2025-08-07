package com.lagab.eventz.app.domain.org.dto.permission;

import java.util.Set;

import com.lagab.eventz.app.domain.org.model.OrganizationPermission;

public record UpdateRolePermissionsDto(
        Set<OrganizationPermission> permissions
) {
}
