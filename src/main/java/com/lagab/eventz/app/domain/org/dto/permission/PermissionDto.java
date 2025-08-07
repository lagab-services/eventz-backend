package com.lagab.eventz.app.domain.org.dto.permission;

import com.lagab.eventz.app.domain.org.model.OrganizationPermission;

public record PermissionDto(
        OrganizationPermission permission,
        String description,
        boolean granted
) {
}
