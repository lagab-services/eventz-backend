package com.lagab.eventz.app.auth.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.lagab.eventz.app.user.entity.Token;
import com.lagab.eventz.app.user.entity.User;
import com.lagab.eventz.app.user.repository.TokenRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService Tests")
class TokenServiceTest {

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private TokenService tokenService;

    private User testUser;
    private String testIpAddress;
    private String testUserAgent;

    @BeforeEach
    void setUp() {
        // Configuration des propriétés
        ReflectionTestUtils.setField(tokenService, "accessTokenExpiration", 3600L);
        ReflectionTestUtils.setField(tokenService, "refreshTokenExpiration", 2592000L);
        ReflectionTestUtils.setField(tokenService, "rememberMeExpiration", 7776000L);
        ReflectionTestUtils.setField(tokenService, "emailVerificationExpiration", 86400L);
        ReflectionTestUtils.setField(tokenService, "passwordResetExpiration", 3600L);

        // Données de test
        testUser = User.builder()
                       .id(1L)
                       .email("test@example.com")
                       .build();
        testIpAddress = "192.168.1.1";
        testUserAgent = "Mozilla/5.0 Test Browser";
    }

    @Nested
    @DisplayName("Access Token Generation")
    class AccessTokenGeneration {

        @Test
        @DisplayName("Should generate access token successfully")
        void shouldGenerateAccessTokenSuccessfully() {
            // Given
            Token savedToken = createMockToken(Token.TokenType.ACCESS_TOKEN);
            when(tokenRepository.save(any(Token.class))).thenReturn(savedToken);
            doNothing().when(tokenRepository).revokeAllUserTokensByType(testUser, Token.TokenType.ACCESS_TOKEN);

            // When
            Token result = tokenService.generateAccessToken(testUser, testIpAddress, testUserAgent);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(Token.TokenType.ACCESS_TOKEN);
            assertThat(result.getUser()).isEqualTo(testUser);
            assertThat(result.getIpAddress()).isEqualTo(testIpAddress);
            assertThat(result.getUserAgent()).isEqualTo(testUserAgent);

            verify(tokenRepository).revokeAllUserTokensByType(testUser, Token.TokenType.ACCESS_TOKEN);
            verify(tokenRepository).save(any(Token.class));
        }

        @Test
        @DisplayName("Should revoke old access tokens before generating new one")
        void shouldRevokeOldAccessTokensBeforeGeneratingNew() {
            // Given
            Token savedToken = createMockToken(Token.TokenType.ACCESS_TOKEN);
            when(tokenRepository.save(any(Token.class))).thenReturn(savedToken);
            doNothing().when(tokenRepository).revokeAllUserTokensByType(testUser, Token.TokenType.ACCESS_TOKEN);

            // When
            tokenService.generateAccessToken(testUser, testIpAddress, testUserAgent);

            // Then
            verify(tokenRepository).revokeAllUserTokensByType(testUser, Token.TokenType.ACCESS_TOKEN);
        }

        @Test
        @DisplayName("Should set correct expiration time for access token")
        void shouldSetCorrectExpirationTimeForAccessToken() {
            // Given
            ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
            Token savedToken = createMockToken(Token.TokenType.ACCESS_TOKEN);
            when(tokenRepository.save(any(Token.class))).thenReturn(savedToken);

            // When
            tokenService.generateAccessToken(testUser, testIpAddress, testUserAgent);

            // Then
            verify(tokenRepository).save(tokenCaptor.capture());
            Token capturedToken = tokenCaptor.getValue();

            LocalDateTime expectedExpiration = LocalDateTime.now().plusSeconds(3600L);
            assertThat(capturedToken.getExpiresAt()).isAfterOrEqualTo(expectedExpiration.minusSeconds(5));
            assertThat(capturedToken.getExpiresAt()).isBeforeOrEqualTo(expectedExpiration.plusSeconds(5));
        }
    }

    @Nested
    @DisplayName("Refresh Token Generation")
    class RefreshTokenGeneration {

        @Test
        @DisplayName("Should generate refresh token with standard expiration")
        void shouldGenerateRefreshTokenWithStandardExpiration() {
            // Given
            Token savedToken = createMockToken(Token.TokenType.REFRESH_TOKEN);
            when(tokenRepository.save(any(Token.class))).thenReturn(savedToken);

            // When
            Token result = tokenService.generateRefreshToken(testUser, testIpAddress, testUserAgent, false);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(Token.TokenType.REFRESH_TOKEN);
            assertThat(result.getUser()).isEqualTo(testUser);

            ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
            verify(tokenRepository).save(tokenCaptor.capture());
            Token capturedToken = tokenCaptor.getValue();

            LocalDateTime expectedExpiration = LocalDateTime.now().plusSeconds(2592000L);
            assertThat(capturedToken.getExpiresAt()).isAfterOrEqualTo(expectedExpiration.minusSeconds(5));
            assertThat(capturedToken.getExpiresAt()).isBeforeOrEqualTo(expectedExpiration.plusSeconds(5));
        }

        @Test
        @DisplayName("Should generate refresh token with remember me expiration")
        void shouldGenerateRefreshTokenWithRememberMeExpiration() {
            // Given
            Token savedToken = createMockToken(Token.TokenType.REFRESH_TOKEN);
            when(tokenRepository.save(any(Token.class))).thenReturn(savedToken);

            // When
            Token result = tokenService.generateRefreshToken(testUser, testIpAddress, testUserAgent, true);

            // Then
            assertThat(result).isNotNull();

            ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
            verify(tokenRepository).save(tokenCaptor.capture());
            Token capturedToken = tokenCaptor.getValue();

            LocalDateTime expectedExpiration = LocalDateTime.now().plusSeconds(7776000L);
            assertThat(capturedToken.getExpiresAt()).isAfterOrEqualTo(expectedExpiration.minusSeconds(5));
            assertThat(capturedToken.getExpiresAt()).isBeforeOrEqualTo(expectedExpiration.plusSeconds(5));
        }
    }

    @Nested
    @DisplayName("Email Verification Token Generation")
    class EmailVerificationTokenGeneration {

        @Test
        @DisplayName("Should generate email verification token successfully")
        void shouldGenerateEmailVerificationTokenSuccessfully() {
            // Given
            Token savedToken = createMockToken(Token.TokenType.EMAIL_VERIFICATION);
            when(tokenRepository.save(any(Token.class))).thenReturn(savedToken);
            doNothing().when(tokenRepository).revokeAllUserTokensByType(testUser, Token.TokenType.EMAIL_VERIFICATION);

            // When
            Token result = tokenService.generateEmailVerificationToken(testUser);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(Token.TokenType.EMAIL_VERIFICATION);
            assertThat(result.getUser()).isEqualTo(testUser);

            verify(tokenRepository).revokeAllUserTokensByType(testUser, Token.TokenType.EMAIL_VERIFICATION);
            verify(tokenRepository).save(any(Token.class));
        }

        @Test
        @DisplayName("Should set correct expiration time for email verification token")
        void shouldSetCorrectExpirationTimeForEmailVerificationToken() {
            // Given
            ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
            Token savedToken = createMockToken(Token.TokenType.EMAIL_VERIFICATION);
            when(tokenRepository.save(any(Token.class))).thenReturn(savedToken);

            // When
            tokenService.generateEmailVerificationToken(testUser);

            // Then
            verify(tokenRepository).save(tokenCaptor.capture());
            Token capturedToken = tokenCaptor.getValue();

            LocalDateTime expectedExpiration = LocalDateTime.now().plusSeconds(86400L);
            assertThat(capturedToken.getExpiresAt()).isAfterOrEqualTo(expectedExpiration.minusSeconds(5));
            assertThat(capturedToken.getExpiresAt()).isBeforeOrEqualTo(expectedExpiration.plusSeconds(5));
        }
    }

    @Nested
    @DisplayName("Password Reset Token Generation")
    class PasswordResetTokenGeneration {

        @Test
        @DisplayName("Should generate password reset token successfully")
        void shouldGeneratePasswordResetTokenSuccessfully() {
            // Given
            Token savedToken = createMockToken(Token.TokenType.PASSWORD_RESET);
            when(tokenRepository.save(any(Token.class))).thenReturn(savedToken);
            doNothing().when(tokenRepository).revokeAllUserTokensByType(testUser, Token.TokenType.PASSWORD_RESET);

            // When
            Token result = tokenService.generatePasswordResetToken(testUser);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(Token.TokenType.PASSWORD_RESET);
            assertThat(result.getUser()).isEqualTo(testUser);

            verify(tokenRepository).revokeAllUserTokensByType(testUser, Token.TokenType.PASSWORD_RESET);
            verify(tokenRepository).save(any(Token.class));
        }

        @Test
        @DisplayName("Should set correct expiration time for password reset token")
        void shouldSetCorrectExpirationTimeForPasswordResetToken() {
            // Given
            ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
            Token savedToken = createMockToken(Token.TokenType.PASSWORD_RESET);
            when(tokenRepository.save(any(Token.class))).thenReturn(savedToken);

            // When
            tokenService.generatePasswordResetToken(testUser);

            // Then
            verify(tokenRepository).save(tokenCaptor.capture());
            Token capturedToken = tokenCaptor.getValue();

            LocalDateTime expectedExpiration = LocalDateTime.now().plusSeconds(3600L);
            assertThat(capturedToken.getExpiresAt()).isAfterOrEqualTo(expectedExpiration.minusSeconds(5));
            assertThat(capturedToken.getExpiresAt()).isBeforeOrEqualTo(expectedExpiration.plusSeconds(5));
        }
    }

    @Nested
    @DisplayName("Token Validation")
    class TokenValidation {

        @Test
        @DisplayName("Should find valid token when exists")
        void shouldFindValidTokenWhenExists() {
            // Given
            String tokenValue = "valid-token";
            Token validToken = createMockToken(Token.TokenType.ACCESS_TOKEN);
            when(tokenRepository.findValidToken(eq(tokenValue), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(validToken));

            // When
            Optional<Token> result = tokenService.findValidToken(tokenValue);

            // Then
            assertThat(result).isPresent().get().isEqualTo(validToken);
            verify(tokenRepository).findValidToken(eq(tokenValue), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should return empty when token not found")
        void shouldReturnEmptyWhenTokenNotFound() {
            // Given
            String tokenValue = "invalid-token";
            when(tokenRepository.findValidToken(eq(tokenValue), any(LocalDateTime.class)))
                    .thenReturn(Optional.empty());

            // When
            Optional<Token> result = tokenService.findValidToken(tokenValue);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should validate token successfully when valid")
        void shouldValidateTokenSuccessfullyWhenValid() {
            // Given
            String tokenValue = "valid-token";
            Token validToken = createMockToken(Token.TokenType.ACCESS_TOKEN);
            when(tokenRepository.findValidToken(eq(tokenValue), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(validToken));

            // When
            boolean result = tokenService.validateToken(tokenValue);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should invalidate token when not valid")
        void shouldInvalidateTokenWhenNotValid() {
            // Given
            String tokenValue = "invalid-token";
            when(tokenRepository.findValidToken(eq(tokenValue), any(LocalDateTime.class)))
                    .thenReturn(Optional.empty());

            // When
            boolean result = tokenService.validateToken(tokenValue);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Token Revocation")
    class TokenRevocation {

        @Test
        @DisplayName("Should revoke token when exists")
        void shouldRevokeTokenWhenExists() {
            // Given
            String tokenValue = "token-to-revoke";
            Token token = createMockToken(Token.TokenType.ACCESS_TOKEN);
            when(tokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(token));
            when(tokenRepository.save(token)).thenReturn(token);

            // When
            tokenService.revokeToken(tokenValue);

            // Then
            verify(tokenRepository).findByToken(tokenValue);
            verify(tokenRepository).save(token);
            assertThat(token.getIsRevoked()).isTrue();
        }

        @Test
        @DisplayName("Should do nothing when token to revoke does not exist")
        void shouldDoNothingWhenTokenToRevokeDoesNotExist() {
            // Given
            String tokenValue = "non-existent-token";
            when(tokenRepository.findByToken(tokenValue)).thenReturn(Optional.empty());

            // When
            tokenService.revokeToken(tokenValue);

            // Then
            verify(tokenRepository).findByToken(tokenValue);
            verify(tokenRepository, times(0)).save(any(Token.class));
        }

        @Test
        @DisplayName("Should revoke tokens by type")
        void shouldRevokeTokensByType() {
            // Given
            Token.TokenType tokenType = Token.TokenType.ACCESS_TOKEN;
            doNothing().when(tokenRepository).revokeAllUserTokensByType(testUser, tokenType);

            // When
            tokenService.revokeTokensByType(testUser, tokenType);

            // Then
            verify(tokenRepository).revokeAllUserTokensByType(testUser, tokenType);
        }

        @Test
        @DisplayName("Should revoke all user tokens")
        void shouldRevokeAllUserTokens() {
            // Given
            doNothing().when(tokenRepository).revokeAllUserTokens(testUser);

            // When
            tokenService.revokeAllUserTokens(testUser);

            // Then
            verify(tokenRepository).revokeAllUserTokens(testUser);
        }
    }

    @Nested
    @DisplayName("Token Usage Management")
    class TokenUsageManagement {

        @Test
        @DisplayName("Should mark token as used")
        void shouldMarkTokenAsUsed() {
            // Given
            Token token = createMockToken(Token.TokenType.EMAIL_VERIFICATION);
            when(tokenRepository.save(token)).thenReturn(token);

            // When
            tokenService.markTokenAsUsed(token);

            // Then
            assertThat(token.getIsUsed()).isTrue();
            verify(tokenRepository).save(token);
        }

        @Test
        @DisplayName("Should get valid tokens by user and type")
        void shouldGetValidTokensByUserAndType() {
            // Given
            Token.TokenType tokenType = Token.TokenType.REFRESH_TOKEN;
            List<Token> expectedTokens = Arrays.asList(
                    createMockToken(tokenType),
                    createMockToken(tokenType)
            );
            when(tokenRepository.findValidTokensByUserAndType(testUser, tokenType))
                    .thenReturn(expectedTokens);

            // When
            List<Token> result = tokenService.getValidTokensByUserAndType(testUser, tokenType);

            // Then
            assertThat(result).isEqualTo(expectedTokens);
            verify(tokenRepository).findValidTokensByUserAndType(testUser, tokenType);
        }

        @Test
        @DisplayName("Should count active access tokens")
        void shouldCountActiveAccessTokens() {
            // Given
            long expectedCount = 3L;
            when(tokenRepository.countActiveAccessTokensByUser(testUser)).thenReturn(expectedCount);

            // When
            long result = tokenService.countActiveAccessTokens(testUser);

            // Then
            assertThat(result).isEqualTo(expectedCount);
            verify(tokenRepository).countActiveAccessTokensByUser(testUser);
        }
    }

    @Nested
    @DisplayName("Token Cleanup")
    class TokenCleanup {

        @Test
        @DisplayName("Should cleanup expired tokens successfully")
        void shouldCleanupExpiredTokensSuccessfully() {
            // Given
            doNothing().when(tokenRepository).deleteExpiredAndOldRevokedTokens(any(LocalDateTime.class), any(LocalDateTime.class));

            // When
            tokenService.cleanupExpiredTokens();

            // Then
            verify(tokenRepository).deleteExpiredAndOldRevokedTokens(any(LocalDateTime.class), any(LocalDateTime.class));
        }

        /**/
        @Test
        @DisplayName("Should handle cleanup exceptions gracefully")
        void shouldHandleCleanupExceptionsGracefully() {
            // Given
            doThrow(new RuntimeException("Database error"))
                    .when(tokenRepository)
                    .deleteExpiredAndOldRevokedTokens(any(LocalDateTime.class), any(LocalDateTime.class));

            // When & Then - should not throw exception
            tokenService.cleanupExpiredTokens();

            verify(tokenRepository).deleteExpiredAndOldRevokedTokens(any(LocalDateTime.class), any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("Token Generation Security")
    class TokenGenerationSecurity {

        @Test
        @DisplayName("Should generate unique tokens")
        void shouldGenerateUniqueTokens() {
            // Given
            Token savedToken1 = createMockToken(Token.TokenType.ACCESS_TOKEN);
            Token savedToken2 = createMockToken(Token.TokenType.ACCESS_TOKEN);
            when(tokenRepository.save(any(Token.class)))
                    .thenReturn(savedToken1)
                    .thenReturn(savedToken2);

            // When
            Token token1 = tokenService.generateAccessToken(testUser, testIpAddress, testUserAgent);
            Token token2 = tokenService.generateAccessToken(testUser, testIpAddress, testUserAgent);

            // Then
            assertThat(token1.getToken()).isNotEqualTo(token2.getToken());
        }

        @Test
        @DisplayName("Should generate tokens with sufficient length")
        void shouldGenerateTokensWithSufficientLength() {
            // Given
            ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
            Token savedToken = createMockToken(Token.TokenType.ACCESS_TOKEN);
            when(tokenRepository.save(any(Token.class))).thenReturn(savedToken);

            // When
            tokenService.generateAccessToken(testUser, testIpAddress, testUserAgent);

            // Then
            verify(tokenRepository).save(tokenCaptor.capture());
            Token capturedToken = tokenCaptor.getValue();

            // Base64 URL-encoded 32 bytes should be 43 characters without padding
            assertThat(capturedToken.getToken()).hasSize(43);
            assertThat(capturedToken.getToken()).matches("^[A-Za-z0-9_-]+$");
        }
    }

    // Méthode utilitaire pour créer des tokens de test
    private Token createMockToken(Token.TokenType type) {
        return Token.builder()
                    .id(1L)
                    .token("test-token-" + System.nanoTime())
                    .type(type)
                    .user(testUser)
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .ipAddress(testIpAddress)
                    .userAgent(testUserAgent)
                    .isRevoked(false)
                    .isUsed(false)
                    .build();
    }
}
