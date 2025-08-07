package com.lagab.eventz.app.domain.org.dto.permission;

import java.util.Set;

import com.lagab.eventz.app.domain.org.model.OrganizationPermission;
import com.lagab.eventz.app.domain.org.model.OrganizationRole;

public record OrganizationRolePermissionsDto(
        OrganizationRole role,
        Set<OrganizationPermission> permissions
) {
}
