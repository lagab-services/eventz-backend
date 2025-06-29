package com.lagab.eventz.app.domain.auth.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.lagab.eventz.app.domain.user.model.Role;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "api_keys")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //@Column(unique = true, nullable = false)
    //private String clientId;

    @Column(unique = true, nullable = false)
    private String clientSecret;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String clientType; // SERVICE_EXTERNE, IA_AGENT, etc.

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    public ApiKey(String name, String clientType, Set<Role> roles) {
        this.name = name;
        this.clientType = clientType;
        this.roles = roles;
        //this.clientId = generateClientId();
        this.clientSecret = generateApiKey();
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }

    /*private String generateClientId() {
        return UUID.randomUUID().toString();
    }*/

    private String generateApiKey() {
        return "ak_" + UUID.randomUUID().toString().replace("-", "");
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return active && !isExpired();
    }
}
