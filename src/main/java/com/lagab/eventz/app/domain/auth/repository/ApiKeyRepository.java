package com.lagab.eventz.app.domain.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lagab.eventz.app.domain.auth.model.ApiKey;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByClientSecretAndActiveTrue(String keyValue);

    List<ApiKey> findByClientTypeAndActiveTrue(String clientType);
}
