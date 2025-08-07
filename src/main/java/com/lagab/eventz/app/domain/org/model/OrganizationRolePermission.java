package com.lagab.eventz.app.domain.org.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "organization_role_permissions")
public class OrganizationRolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private String organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private OrganizationRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false)
    private OrganizationPermission permission;

    @Column(name = "granted", nullable = false)
    private Boolean granted = true;

    public OrganizationRolePermission(String organizationId, OrganizationRole role, OrganizationPermission permission, Boolean granted) {
        this.organizationId = organizationId;
        this.role = role;
        this.permission = permission;
        this.granted = granted;
    }
}
