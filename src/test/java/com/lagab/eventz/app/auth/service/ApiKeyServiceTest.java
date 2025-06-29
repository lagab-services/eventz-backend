package com.lagab.eventz.app.auth.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.lagab.eventz.app.auth.entity.ApiKey;
import com.lagab.eventz.app.auth.repository.ApiKeyRepository;
import com.lagab.eventz.app.user.entity.Role;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ApiKeyService apiKeyService;

    private final String testName = "test-api-key";
    private final String testClientType = "WEB";
    private final Set<Role> testRoles = Collections.emptySet();
    private final LocalDateTime testExpiresAt = LocalDateTime.now().plusDays(30);
    private final String rawKeyValue = "ak_" + UUID.randomUUID().toString().replace("-", "");
    private final String hashedKeyValue = "hashed_" + rawKeyValue;

    @Test
    void createApiKey_shouldGenerateAndSaveKey() {
        // Given
        ApiKey expectedApiKey = ApiKey.builder()
                                      .name(testName)
                                      .clientType(testClientType)
                                      .roles(testRoles)
                                      .expiresAt(testExpiresAt)
                                      //.clientId(UUID.randomUUID().toString())
                                      .clientSecret(hashedKeyValue)
                                      .build();

        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(expectedApiKey);

        // When
        ApiKey result = apiKeyService.createApiKey(testName, testClientType, testRoles, testExpiresAt);

        // Then
        assertNotNull(result);
        assertEquals(testName, result.getName());
        assertEquals(testClientType, result.getClientType());
        assertEquals(testRoles, result.getRoles());
        assertEquals(testExpiresAt, result.getExpiresAt());
        assertEquals(hashedKeyValue, result.getClientSecret());

        verify(passwordEncoder).encode(anyString());
        verify(apiKeyRepository).save(any(ApiKey.class));
    }

    @Test
    void validateApiKey_shouldReturnEmptyForInvalidKey() {
        // Given
        when(apiKeyRepository.findByClientSecretAndActiveTrue(anyString())).thenReturn(Optional.empty());

        // When
        Optional<ApiKey> result = apiKeyService.validateApiKey("invalid_key");

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void validateApiKey_shouldReturnKeyWhenValid() {
        // Given
        ApiKey validKey = ApiKey.builder()
                                //.clientId(UUID.randomUUID().toString())
                                .clientSecret(hashedKeyValue)
                                .active(true)
                                .expiresAt(LocalDateTime.now().plusDays(1))
                                .build();

        when(apiKeyRepository.findByClientSecretAndActiveTrue(rawKeyValue)).thenReturn(Optional.of(validKey));

        // When
        Optional<ApiKey> result = apiKeyService.validateApiKey(rawKeyValue);

        // Then
        assertTrue(result.isPresent());
        assertEquals(validKey, result.get());
    }

    @Test
    void revokeApiKey_shouldDeactivateKey() {
        // Given
        ApiKey activeKey = ApiKey.builder()
                                 .id(1L)
                                 .name(testName)
                                 .active(true)
                                 .build();

        when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(activeKey));

        // When
        apiKeyService.revokeApiKey(1L);

        // Then
        assertFalse(activeKey.isActive());
        verify(apiKeyRepository).save(activeKey);
    }

    @Test
    void revokeApiKey_shouldDoNothingWhenKeyNotFound() {
        // Given
        when(apiKeyRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When
        apiKeyService.revokeApiKey(999L);

        // Then
        verify(apiKeyRepository, never()).save(any());
    }

    @Test
    void getApiKeysByType_shouldFilterByClientType() {
        // Given
        when(apiKeyRepository.findByClientTypeAndActiveTrue(testClientType))
                .thenReturn(Collections.singletonList(new ApiKey()));

        // When
        List<ApiKey> result = apiKeyService.getApiKeysByType(testClientType);

        // Then
        assertEquals(1, result.size());
        verify(apiKeyRepository).findByClientTypeAndActiveTrue(testClientType);
        verify(apiKeyRepository, never()).findAll();
    }

    @Test
    void getApiKeysByType_shouldReturnAllWhenNoTypeSpecified() {
        // Given
        when(apiKeyRepository.findAll())
                .thenReturn(Collections.singletonList(new ApiKey()));

        // When
        List<ApiKey> result = apiKeyService.getApiKeysByType(null);

        // Then
        assertEquals(1, result.size());
        verify(apiKeyRepository).findAll();
        verify(apiKeyRepository, never()).findByClientTypeAndActiveTrue(any());
    }

    @Test
    void validateApiKey_shouldFilterExpiredKeys() {
        // Given
        ApiKey expiredKey = ApiKey.builder()
                                  //.clientId(UUID.randomUUID().toString())
                                  .clientSecret(hashedKeyValue)
                                  .active(true)
                                  .expiresAt(LocalDateTime.now().minusDays(1))
                                  .build();

        when(apiKeyRepository.findByClientSecretAndActiveTrue(rawKeyValue)).thenReturn(Optional.of(expiredKey));

        // When
        Optional<ApiKey> result = apiKeyService.validateApiKey(rawKeyValue);

        // Then
        assertTrue(result.isEmpty());
    }
}
