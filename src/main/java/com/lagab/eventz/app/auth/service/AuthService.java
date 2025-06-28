package com.lagab.eventz.app.auth.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lagab.eventz.app.auth.dto.AuthResponse;
import com.lagab.eventz.app.auth.dto.ChangePasswordRequest;
import com.lagab.eventz.app.auth.dto.ForgotPasswordRequest;
import com.lagab.eventz.app.auth.dto.LoginRequestDto;
import com.lagab.eventz.app.auth.dto.RefreshTokenRequest;
import com.lagab.eventz.app.auth.dto.RegisterRequest;
import com.lagab.eventz.app.auth.dto.ResetPasswordRequest;
import com.lagab.eventz.app.auth.dto.TokenValidationResponse;
import com.lagab.eventz.app.auth.dto.UserResponse;
import com.lagab.eventz.app.common.exception.AuthenticationException;
import com.lagab.eventz.app.common.exception.ResourceNotFoundException;
import com.lagab.eventz.app.common.exception.ValidationException;
import com.lagab.eventz.app.user.entity.Role;
import com.lagab.eventz.app.user.entity.Token;
import com.lagab.eventz.app.user.entity.User;
import com.lagab.eventz.app.user.mapper.UserMapper;
import com.lagab.eventz.app.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequestDto request, String ipAddress, String userAgent) {
        try {
            // Authentication with Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            User user = (User) authentication.getPrincipal();

            // Check if user is active
            if (!user.getIsActive()) {
                throw new AuthenticationException("Account is disabled");
            }

            // Generate tokens
            String accessTokenString = jwtService.generateAccessToken(
                    user.getId(),
                    Map.of("roles", List.of(user.getRole()))
                    //Map.of("roles", user.getRoles().stream().map(role -> role.getName()).toList())
            );
            Token refreshToken = tokenService.generateRefreshToken(user, ipAddress, userAgent,
                    request.rememberMe() != null && request.rememberMe());

            log.debug("Successful login for user: {}", user.getEmail());

            return new AuthResponse(
                    accessTokenString,
                    refreshToken.getToken(),
                    "Bearer",
                    jwtService.getAccessTokenValidityInMilliseconds(),
                    userMapper.toResponse(user)
            );

        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for: {}", request.email());
            throw new AuthenticationException("Incorrect email or password");
        }
    }

    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new ValidationException("An account already exists with this email");
        }

        // Create user
        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user.setIsActive(true);
        user.setIsEmailVerified(false);

        user = userRepository.save(user);

        // Generate email verification token
        tokenService.generateEmailVerificationToken(user);

        log.info("New user registered: {}", user.getEmail());

        // For demo purposes, we return tokens directly
        // In production, you might want to enforce email verification first
        Token accessToken = tokenService.generateAccessToken(user, null, null);
        Token refreshToken = tokenService.generateRefreshToken(user, null, null, false);

        return new AuthResponse(
                accessToken.getToken(),
                refreshToken.getToken(),
                "Bearer",
                accessToken.getExpiresAt().toEpochSecond(java.time.ZoneOffset.UTC) -
                        java.time.LocalDateTime.now().toEpochSecond(java.time.ZoneOffset.UTC),
                userMapper.toResponse(user)
        );
    }

    public AuthResponse refreshToken(RefreshTokenRequest request, String ipAddress, String userAgent) {
        Token refreshToken = tokenService.findValidToken(request.refreshToken())
                                         .orElseThrow(() -> new AuthenticationException("Invalid or expired refresh token"));

        if (refreshToken.getType() != Token.TokenType.REFRESH_TOKEN) {
            throw new AuthenticationException("Invalid token type");
        }

        User user = refreshToken.getUser();

        // Verify if user is still active
        if (!user.getIsActive()) {
            throw new AuthenticationException("Account is disabled");
        }

        // Generate new tokens
        Token newAccessToken = tokenService.generateAccessToken(user, ipAddress, userAgent);
        Token newRefreshToken = tokenService.generateRefreshToken(user, ipAddress, userAgent, false);

        // Revoke the old refresh token
        tokenService.revokeToken(request.refreshToken());

        log.info("Token refreshed for user: {}", user.getEmail());

        return new AuthResponse(
                newAccessToken.getToken(),
                newRefreshToken.getToken(),
                "Bearer",
                newAccessToken.getExpiresAt().toEpochSecond(java.time.ZoneOffset.UTC) -
                        java.time.LocalDateTime.now().toEpochSecond(java.time.ZoneOffset.UTC),
                userMapper.toResponse(user)
        );
    }

    public void logout(String tokenValue) {
        Optional<Token> tokenOpt = tokenService.findValidToken(tokenValue);

        if (tokenOpt.isPresent()) {
            Token token = tokenOpt.get();
            User user = token.getUser();

            // Revoke all user tokens
            tokenService.revokeAllUserTokens(user);

            log.info("User logged out: {}", user.getEmail());
        }
    }

    public void logoutFromDevice(String tokenValue) {
        Optional<Token> tokenOpt = tokenService.findValidToken(tokenValue);

        if (tokenOpt.isPresent()) {
            Token token = tokenOpt.get();

            // Revoke only this token
            tokenService.revokeToken(tokenValue);

            log.info("Logged out from one device for user: {}", token.getUser().getEmail());
        }
    }

    public TokenValidationResponse validateToken(String tokenValue) {
        Optional<Token> tokenOpt = tokenService.findValidToken(tokenValue);

        if (tokenOpt.isEmpty()) {
            return new TokenValidationResponse(false, null, null);
        }

        Token token = tokenOpt.get();
        User user = token.getUser();

        return new TokenValidationResponse(
                true,
                token.getExpiresAt(),
                userMapper.toResponse(user)
        );
    }

    public UserResponse getCurrentUser(String tokenValue) {
        Token token = tokenService.findValidToken(tokenValue)
                                  .orElseThrow(() -> new AuthenticationException("Invalid token"));

        return userMapper.toResponse(token.getUser());
    }

    public UserResponse getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = (User) principal;
        return userMapper.toResponse(currentUser);
    }

    public void changePassword(ChangePasswordRequest request) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        User user = (User) principal;

        // Verify current password
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ValidationException("Current password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Revoke all existing tokens to force re-login
        tokenService.revokeAllUserTokens(user);

        log.info("Password changed for user: {}", user.getEmail());
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.email());

        if (userOpt.isEmpty()) {
            // For security reasons, we don't reveal if the email exists
            log.warn("Password reset request for non-existent email: {}", request.email());
            return;
        }

        User user = userOpt.get();

        if (!user.getIsActive()) {
            log.warn("Password reset request for disabled account: {}", request.email());
            return;
        }

        // Generate reset token
        Token resetToken = tokenService.generatePasswordResetToken(user);

        emailService.sendPasswordResetEmail(user, resetToken.getToken());

        log.info("Password reset token generated for: {}", user.getEmail());
    }

    public void resetPassword(ResetPasswordRequest request) {
        Token resetToken = tokenService.findValidToken(request.token())
                                       .orElseThrow(() -> new ValidationException("Invalid or expired reset token"));

        if (resetToken.getType() != Token.TokenType.PASSWORD_RESET) {
            throw new ValidationException("Invalid token type");
        }

        User user = resetToken.getUser();

        // Update password
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Mark token as used
        tokenService.markTokenAsUsed(resetToken);

        // Revoke all other tokens
        tokenService.revokeAllUserTokens(user);

        log.info("Password reset for user: {}", user.getEmail());
    }

    public void verifyEmail(String tokenValue) {
        Token verificationToken = tokenService.findValidToken(tokenValue)
                                              .orElseThrow(() -> new ValidationException("Invalid or expired verification token"));

        if (verificationToken.getType() != Token.TokenType.EMAIL_VERIFICATION) {
            throw new ValidationException("Invalid token type");
        }

        User user = verificationToken.getUser();
        user.setIsEmailVerified(true);
        userRepository.save(user);

        // Mark token as used
        tokenService.markTokenAsUsed(verificationToken);

        log.info("Email verified for user: {}", user.getEmail());
    }

    public void resendEmailVerification(String email) {
        User user = userRepository.findByEmail(email)
                                  .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getIsEmailVerified()) {
            throw new ValidationException("Email already verified");
        }

        // Generate new verification token
        Token verificationToken = tokenService.generateEmailVerificationToken(user);

        emailService.sendValidationEmail(user, verificationToken.getToken());

        log.info("New email verification token generated for: {}", user.getEmail());
    }
}
