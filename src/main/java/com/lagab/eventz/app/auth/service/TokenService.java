package com.lagab.eventz.app.auth.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lagab.eventz.app.user.entity.Token;
import com.lagab.eventz.app.user.entity.User;
import com.lagab.eventz.app.user.repository.TokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TokenService {

    private final TokenRepository tokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.auth.access-token-expiration:3600}") // 1 heure par défaut
    private Long accessTokenExpiration;

    @Value("${app.auth.refresh-token-expiration:2592000}") // 30 jours par défaut
    private Long refreshTokenExpiration;

    @Value("${app.auth.remember-me-expiration:7776000}") // 90 jours par défaut
    private Long rememberMeExpiration;

    @Value("${app.auth.email-verification-expiration:86400}") // 24 heures par défaut
    private Long emailVerificationExpiration;

    @Value("${app.auth.password-reset-expiration:3600}") // 1 heure par défaut
    private Long passwordResetExpiration;

    public Token generateAccessToken(User user, String ipAddress, String userAgent) {
        // Révoquer les anciens access tokens
        revokeTokensByType(user, Token.TokenType.ACCESS_TOKEN);

        String tokenValue = generateSecureToken();

        Token token = Token.builder()
                           .token(tokenValue)
                           .type(Token.TokenType.ACCESS_TOKEN)
                           .user(user)
                           .expiresAt(LocalDateTime.now().plusSeconds(accessTokenExpiration))
                           .ipAddress(ipAddress)
                           .userAgent(userAgent)
                           .build();

        return tokenRepository.save(token);
    }

    public Token generateRefreshToken(User user, String ipAddress, String userAgent, boolean rememberMe) {
        String tokenValue = generateSecureToken();
        long expiration = rememberMe ? rememberMeExpiration : refreshTokenExpiration;

        Token token = Token.builder()
                           .token(tokenValue)
                           .type(Token.TokenType.REFRESH_TOKEN)
                           .user(user)
                           .expiresAt(LocalDateTime.now().plusSeconds(expiration))
                           .ipAddress(ipAddress)
                           .userAgent(userAgent)
                           .build();

        return tokenRepository.save(token);
    }

    public Token generateEmailVerificationToken(User user) {
        // Revoque old verification token
        revokeTokensByType(user, Token.TokenType.EMAIL_VERIFICATION);

        String tokenValue = generateSecureToken();

        Token token = Token.builder()
                           .token(tokenValue)
                           .type(Token.TokenType.EMAIL_VERIFICATION)
                           .user(user)
                           .expiresAt(LocalDateTime.now().plusSeconds(emailVerificationExpiration))
                           .build();

        return tokenRepository.save(token);
    }

    public Token generatePasswordResetToken(User user) {
        // Revoque old reset token
        revokeTokensByType(user, Token.TokenType.PASSWORD_RESET);

        String tokenValue = generateSecureToken();

        Token token = Token.builder()
                           .token(tokenValue)
                           .type(Token.TokenType.PASSWORD_RESET)
                           .user(user)
                           .expiresAt(LocalDateTime.now().plusSeconds(passwordResetExpiration))
                           .build();

        return tokenRepository.save(token);
    }

    public Optional<Token> findValidToken(String tokenValue) {
        return tokenRepository.findValidToken(tokenValue, LocalDateTime.now());
    }

    public boolean validateToken(String tokenValue) {
        return findValidToken(tokenValue).isPresent();
    }

    public void revokeToken(String tokenValue) {
        tokenRepository.findByToken(tokenValue)
                       .ifPresent(token -> {
                           token.setIsRevoked(true);
                           tokenRepository.save(token);
                       });
    }

    public void revokeTokensByType(User user, Token.TokenType type) {
        tokenRepository.revokeAllUserTokensByType(user, type);
    }

    public void revokeAllUserTokens(User user) {
        tokenRepository.revokeAllUserTokens(user);
    }

    public void markTokenAsUsed(Token token) {
        token.setIsUsed(true);
        tokenRepository.save(token);
    }

    public List<Token> getValidTokensByUserAndType(User user, Token.TokenType type) {
        return tokenRepository.findValidTokensByUserAndType(user, type);
    }

    public long countActiveAccessTokens(User user) {
        return tokenRepository.countActiveAccessTokensByUser(user);
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    // Automatic cleanup of expired tokens (runs every hour)
    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredTokens() {
        log.info("Cleaning up expired tokens...");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoffDate = now.minusDays(7); // Delete revoked tokens older than 7 days

        try {
            tokenRepository.deleteExpiredAndOldRevokedTokens(now, cutoffDate);
            log.info("Token cleanup completed");
        } catch (Exception e) {
            log.error("Error during token cleanup", e);
        }
    }
}
