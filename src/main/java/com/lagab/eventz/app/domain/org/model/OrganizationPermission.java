package com.lagab.eventz.app.domain.org.model;

import lombok.Getter;

@Getter
public enum OrganizationPermission {
    // Organization management
    ORGANIZATION_VIEW("organization.view", "View organization details"),
    ORGANIZATION_EDIT("organization.edit", "Edit organization details"),
    ORGANIZATION_DELETE("organization.delete", "Delete organization"),
    ORGANIZATION_ARCHIVE("organization.archive", "Archive/restore organization"),

    // Member management
    MEMBER_VIEW("member.view", "View organization members"),
    MEMBER_INVITE("member.invite", "Invite new members"),
    MEMBER_EDIT_ROLE("member.edit_role", "Edit member roles"),
    MEMBER_REMOVE("member.remove", "Remove members"),

    // Statistics and analytics
    STATS_VIEW("stats.view", "View organization statistics"),
    STATS_EXPORT("stats.export", "Export organization data"),

    // Event management (si applicable)
    EVENT_CREATE("event.create", "Create events"),
    EVENT_EDIT("event.edit", "Edit events"),
    EVENT_DELETE("event.delete", "Delete events"),
    EVENT_PUBLISH("event.publish", "Publish events"),

    // Financial (si applicable)
    FINANCE_VIEW("finance.view", "View financial data"),
    FINANCE_MANAGE("finance.manage", "Manage finances"),

    // Settings
    SETTINGS_VIEW("settings.view", "View organization settings"),
    SETTINGS_MANAGE("settings.manage", "Manage organization settings");

    private final String permission;
    private final String description;

    OrganizationPermission(String permission, String description) {
        this.permission = permission;
        this.description = description;
    }
    
}
