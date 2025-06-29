package com.lagab.eventz.app.domain.auth.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lagab.eventz.app.domain.auth.model.ApiKey;
import com.lagab.eventz.app.domain.auth.repository.ApiKeyRepository;
import com.lagab.eventz.app.domain.user.model.Role;
import com.lagab.eventz.app.interfaces.web.auth.dto.ApiKeyResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    public ApiKey createApiKey(String name, String clientType, Set<Role> roles, LocalDateTime expiresAt) {
        ApiKey apiKey = new ApiKey(name, clientType, roles);
        apiKey.setExpiresAt(expiresAt);

        // Hash the API key for secure storage
        apiKey.setClientSecret(passwordEncoder.encode(apiKey.getClientSecret()));

        log.info("Creating new API key: {} for client: {}", name, clientType);
        return apiKeyRepository.save(apiKey);
    }

    public Optional<ApiKey> validateApiKey(String keyValue) {
        return apiKeyRepository.findByClientSecretAndActiveTrue(keyValue)
                               .filter(ApiKey::isValid);
    }

    public void revokeApiKey(Long id) {
        apiKeyRepository.findById(id).ifPresent(apiKey -> {
            apiKey.setActive(false);
            apiKeyRepository.save(apiKey);
            log.info("API key revoked: {}", apiKey.getName());
        });
    }

    public List<ApiKey> getApiKeysByType(String clientType) {
        return clientType != null ?
                apiKeyRepository.findByClientTypeAndActiveTrue(clientType) :
                apiKeyRepository.findAll();
    }

    public ApiKeyResponse getApikeyById(Long id) {
        ApiKey apiKey = apiKeyRepository.getReferenceById(id);
        return ApiKeyResponse.withoutKey(apiKey);
    }
}
