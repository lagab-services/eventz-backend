package com.lagab.eventz.app.auth.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.lagab.eventz.app.common.exception.AuthenticationException;
import com.lagab.eventz.app.common.exception.ResourceNotFoundException;
import com.lagab.eventz.app.common.exception.ValidationException;
import com.lagab.eventz.app.domain.auth.dto.AuthResponse;
import com.lagab.eventz.app.domain.auth.dto.ChangePasswordRequest;
import com.lagab.eventz.app.domain.auth.dto.ForgotPasswordRequest;
import com.lagab.eventz.app.domain.auth.dto.LoginRequestDto;
import com.lagab.eventz.app.domain.auth.dto.RefreshTokenRequest;
import com.lagab.eventz.app.domain.auth.dto.RegisterRequest;
import com.lagab.eventz.app.domain.auth.dto.ResetPasswordRequest;
import com.lagab.eventz.app.domain.auth.dto.TokenValidationResponse;
import com.lagab.eventz.app.domain.auth.dto.UserResponse;
import com.lagab.eventz.app.domain.auth.service.AuthService;
import com.lagab.eventz.app.domain.auth.service.EmailService;
import com.lagab.eventz.app.domain.auth.service.JwtService;
import com.lagab.eventz.app.domain.auth.service.TokenService;
import com.lagab.eventz.app.domain.org.service.OrganizationService;
import com.lagab.eventz.app.domain.user.mapper.UserMapper;
import com.lagab.eventz.app.domain.user.model.Role;
import com.lagab.eventz.app.domain.user.model.Token;
import com.lagab.eventz.app.domain.user.model.Token.TokenType;
import com.lagab.eventz.app.domain.user.model.User;
import com.lagab.eventz.app.domain.user.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Authentication authentication;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private EmailService emailService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private Token testAccessToken;
    private Token testRefreshToken;
    private UserResponse testUserResponse;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("encoded-password");
        testUser.setRole(Role.USER);
        testUser.setIsActive(true);
        testUser.setIsEmailVerified(true);

        testAccessToken = new Token();
        testAccessToken.setToken("access-token");
        testAccessToken.setType(TokenType.ACCESS_TOKEN);
        testAccessToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        testAccessToken.setUser(testUser);

        testRefreshToken = new Token();
        testRefreshToken.setToken("refresh-token");
        testRefreshToken.setType(TokenType.REFRESH_TOKEN);
        testRefreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        testRefreshToken.setUser(testUser);

        testUserResponse = new UserResponse(1L, "test@example.com", "John", "Doe", Role.USER.name(), "en-EN", true, true, LocalDateTime.now());
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLoginSuccessfully() {
            // Given
            LoginRequestDto request = new LoginRequestDto("test@example.com", "password", false);
            String ipAddress = "192.168.1.1";
            String userAgent = "Mozilla/5.0";

            when(jwtService.getAccessTokenValidityInMilliseconds()).thenReturn(360000L);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(tokenService.generateRefreshToken(testUser, ipAddress, userAgent, false)).thenReturn(testRefreshToken);
            when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

            // When
            AuthResponse response = authService.login(request, ipAddress, userAgent);

            // Then
            assertNotNull(response);
            assertEquals("refresh-token", response.refreshToken());
            assertEquals("Bearer", response.tokenType());
            assertEquals(testUserResponse, response.user());
            assertTrue(response.expiresIn() > 0);

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(tokenService).generateRefreshToken(testUser, ipAddress, userAgent, false);
        }

        @Test
        @DisplayName("Should login with remember me option")
        void shouldLoginWithRememberMe() {
            // Given
            LoginRequestDto request = new LoginRequestDto("test@example.com", "password", true);
            String ipAddress = "192.168.1.1";
            String userAgent = "Mozilla/5.0";

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(tokenService.generateRefreshToken(testUser, ipAddress, userAgent, true)).thenReturn(testRefreshToken);
            when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

            // When
            authService.login(request, ipAddress, userAgent);

            // Then
            verify(tokenService).generateRefreshToken(testUser, ipAddress, userAgent, true);
        }

        @Test
        @DisplayName("Should throw exception for bad credentials")
        void shouldThrowExceptionForBadCredentials() {
            // Given
            LoginRequestDto request = new LoginRequestDto("test@example.com", "wrong-password", false);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // When & Then
            AuthenticationException exception = assertThrows(AuthenticationException.class,
                    () -> authService.login(request, "192.168.1.1", "Mozilla/5.0"));

            assertEquals("Incorrect email or password", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Register Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should register new user successfully")
        void shouldRegisterNewUserSuccessfully() {
            // Given
            RegisterRequest request = new RegisterRequest("new@example.com", "password", "John", "Doe");

            when(userRepository.existsByEmail(request.email())).thenReturn(false);
            when(userMapper.toEntity(request)).thenReturn(testUser);
            when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(tokenService.generateEmailVerificationToken(testUser)).thenReturn(testAccessToken);
            when(tokenService.generateAccessToken(testUser, null, null)).thenReturn(testAccessToken);
            when(tokenService.generateRefreshToken(testUser, null, null, false)).thenReturn(testRefreshToken);
            when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

            // When
            AuthResponse response = authService.register(request);

            // Then
            assertNotNull(response);
            assertEquals("access-token", response.accessToken());
            assertEquals("refresh-token", response.refreshToken());

            verify(userRepository).existsByEmail(request.email());
            verify(passwordEncoder).encode(request.password());
            verify(userRepository).save(any(User.class));
            verify(tokenService).generateEmailVerificationToken(testUser);
        }

        @Test
        @DisplayName("Should throw exception for existing email")
        void shouldThrowExceptionForExistingEmail() {
            // Given
            RegisterRequest request = new RegisterRequest("existing@example.com", "password", "John", "Doe");

            when(userRepository.existsByEmail(request.email())).thenReturn(true);

            // When & Then
            ValidationException exception = assertThrows(ValidationException.class,
                    () -> authService.register(request));

            assertEquals("An account already exists with this email", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Refresh Token Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should refresh token successfully")
        void shouldRefreshTokenSuccessfully() {
            // Given
            RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");
            String ipAddress = "192.168.1.1";
            String userAgent = "Mozilla/5.0";

            when(tokenService.findValidToken(request.refreshToken())).thenReturn(Optional.of(testRefreshToken));
            when(tokenService.generateAccessToken(testUser, ipAddress, userAgent)).thenReturn(testAccessToken);
            when(tokenService.generateRefreshToken(testUser, ipAddress, userAgent, false)).thenReturn(testRefreshToken);
            when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

            // When
            AuthResponse response = authService.refreshToken(request, ipAddress, userAgent);

            // Then
            assertNotNull(response);
            assertEquals("access-token", response.accessToken());
            verify(tokenService).revokeToken(request.refreshToken());
        }

        @Test
        @DisplayName("Should throw exception for invalid refresh token")
        void shouldThrowExceptionForInvalidRefreshToken() {
            // Given
            RefreshTokenRequest request = new RefreshTokenRequest("invalid-token");

            when(tokenService.findValidToken(request.refreshToken())).thenReturn(Optional.empty());

            // When & Then
            AuthenticationException exception = assertThrows(AuthenticationException.class,
                    () -> authService.refreshToken(request, "192.168.1.1", "Mozilla/5.0"));

            assertEquals("Invalid or expired refresh token", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for wrong token type")
        void shouldThrowExceptionForWrongTokenType() {
            // Given
            RefreshTokenRequest request = new RefreshTokenRequest("access-token");
            testAccessToken.setType(TokenType.ACCESS_TOKEN);

            when(tokenService.findValidToken(request.refreshToken())).thenReturn(Optional.of(testAccessToken));

            // When & Then
            AuthenticationException exception = assertThrows(AuthenticationException.class,
                    () -> authService.refreshToken(request, "192.168.1.1", "Mozilla/5.0"));

            assertEquals("Invalid token type", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Logout Tests")
    class LogoutTests {

        @Test
        @DisplayName("Should logout successfully")
        void shouldLogoutSuccessfully() {
            // Given
            String tokenValue = "valid-token";

            when(tokenService.findValidToken(tokenValue)).thenReturn(Optional.of(testAccessToken));

            // When
            authService.logout(tokenValue);

            // Then
            verify(tokenService).revokeAllUserTokens(testUser);
        }

        @Test
        @DisplayName("Should handle logout with invalid token gracefully")
        void shouldHandleLogoutWithInvalidTokenGracefully() {
            // Given
            String tokenValue = "invalid-token";

            when(tokenService.findValidToken(tokenValue)).thenReturn(Optional.empty());

            // When & Then
            assertDoesNotThrow(() -> authService.logout(tokenValue));
            verify(tokenService, never()).revokeAllUserTokens(any());
        }

        @Test
        @DisplayName("Should logout from device successfully")
        void shouldLogoutFromDeviceSuccessfully() {
            // Given
            String tokenValue = "valid-token";

            when(tokenService.findValidToken(tokenValue)).thenReturn(Optional.of(testAccessToken));

            // When
            authService.logoutFromDevice(tokenValue);

            // Then
            verify(tokenService).revokeToken(tokenValue);
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTests {

        @Test
        @DisplayName("Should validate token successfully")
        void shouldValidateTokenSuccessfully() {
            // Given
            String tokenValue = "valid-token";

            when(tokenService.findValidToken(tokenValue)).thenReturn(Optional.of(testAccessToken));
            when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

            // When
            TokenValidationResponse response = authService.validateToken(tokenValue);

            // Then
            assertTrue(response.isValid());
            assertEquals(testAccessToken.getExpiresAt(), response.expiresAt());
            assertEquals(testUserResponse, response.user());
        }

        @Test
        @DisplayName("Should return invalid for non-existent token")
        void shouldReturnInvalidForNonExistentToken() {
            // Given
            String tokenValue = "invalid-token";

            when(tokenService.findValidToken(tokenValue)).thenReturn(Optional.empty());

            // When
            TokenValidationResponse response = authService.validateToken(tokenValue);

            // Then
            assertFalse(response.isValid());
            assertNull(response.expiresAt());
            assertNull(response.user());
        }
    }

    @Nested
    @DisplayName("Get Current User Tests")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Should get current user successfully")
        void shouldGetCurrentUserSuccessfully() {
            // Given
            String tokenValue = "valid-token";

            when(tokenService.findValidToken(tokenValue)).thenReturn(Optional.of(testAccessToken));
            when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

            // When
            UserResponse response = authService.getCurrentUser(tokenValue);

            // Then
            assertEquals(testUserResponse, response);
        }

        @Test
        @DisplayName("Should throw exception for invalid token")
        void shouldThrowExceptionForInvalidToken() {
            // Given
            String tokenValue = "invalid-token";

            when(tokenService.findValidToken(tokenValue)).thenReturn(Optional.empty());

            // When & Then
            AuthenticationException exception = assertThrows(AuthenticationException.class,
                    () -> authService.getCurrentUser(tokenValue));

            assertEquals("Invalid token", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Change Password Tests")
    class ChangePasswordTests {

        @Test
        @DisplayName("Should change password successfully")
        void shouldChangePasswordSuccessfully() {
            // Given
            String rawCurrentPassword = "currentPassword123";
            String rawNewPassword = "newPassword456";
            String encodedNewPassword = "encodedNewPassword";
            String oldPassword = testUser.getPassword();

            ChangePasswordRequest request = new ChangePasswordRequest(rawCurrentPassword, rawNewPassword);

            // Mock authentication
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    testUser,
                    null,
                    testUser.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            when(passwordEncoder.matches(rawCurrentPassword, testUser.getPassword())).thenReturn(true);
            when(passwordEncoder.encode(rawNewPassword)).thenReturn(encodedNewPassword);

            // When
            authService.changePassword(request);

            // Then
            // Verify password encoder was called correctly
            verify(passwordEncoder).matches(rawCurrentPassword, oldPassword);
            verify(passwordEncoder).encode(rawNewPassword);

            // Verify the user's password was updated with the new encoded password
            assertEquals(encodedNewPassword, testUser.getPassword());

            // Verify the user was saved
            verify(userRepository).save(testUser);

            // Verify all tokens were revoked
            verify(tokenService).revokeAllUserTokens(testUser);
        }

        @Test
        @DisplayName("Should throw exception for incorrect current password")
        void shouldThrowExceptionForIncorrectCurrentPassword() {
            // Given
            ChangePasswordRequest request = new ChangePasswordRequest("wrong-password", "new-password");
            User testUser = new User(); // or use your test user setup
            testUser.setPassword("encoded-current-password");

            // Mock authentication
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    testUser,
                    null,
                    testUser.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            when(passwordEncoder.matches(request.currentPassword(), testUser.getPassword())).thenReturn(false);

            // When & Then
            ValidationException exception = assertThrows(ValidationException.class,
                    () -> authService.changePassword(request));

            assertEquals("Current password is incorrect", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Forgot Password Tests")
    class ForgotPasswordTests {

        @Test
        @DisplayName("Should generate reset token for existing user")
        void shouldGenerateResetTokenForExistingUser() {
            // Given
            ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");
            Token resetToken = new Token();
            resetToken.setType(TokenType.PASSWORD_RESET);

            when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(testUser));
            when(tokenService.generatePasswordResetToken(testUser)).thenReturn(resetToken);

            // When
            authService.forgotPassword(request);

            // Then
            verify(tokenService).generatePasswordResetToken(testUser);
        }

        @Test
        @DisplayName("Should handle non-existent email gracefully")
        void shouldHandleNonExistentEmailGracefully() {
            // Given
            ForgotPasswordRequest request = new ForgotPasswordRequest("nonexistent@example.com");

            when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

            // When & Then
            assertDoesNotThrow(() -> authService.forgotPassword(request));
            verify(tokenService, never()).generatePasswordResetToken(any());
        }

        @Test
        @DisplayName("Should handle inactive user gracefully")
        void shouldHandleInactiveUserGracefully() {
            // Given
            ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");
            testUser.setIsActive(false);

            when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(testUser));

            // When & Then
            assertDoesNotThrow(() -> authService.forgotPassword(request));
            verify(tokenService, never()).generatePasswordResetToken(any());
        }
    }

    @Nested
    @DisplayName("Reset Password Tests")
    class ResetPasswordTests {

        @Test
        @DisplayName("Should reset password successfully")
        void shouldResetPasswordSuccessfully() {
            // Given
            ResetPasswordRequest request = new ResetPasswordRequest("reset-token", "new-password");
            Token resetToken = new Token();
            resetToken.setType(TokenType.PASSWORD_RESET);
            resetToken.setUser(testUser);

            when(tokenService.findValidToken(request.token())).thenReturn(Optional.of(resetToken));
            when(passwordEncoder.encode(request.newPassword())).thenReturn("new-encoded-password");
            when(userRepository.save(testUser)).thenReturn(testUser);

            // When
            authService.resetPassword(request);

            // Then
            verify(passwordEncoder).encode(request.newPassword());
            verify(userRepository).save(testUser);
            verify(tokenService).markTokenAsUsed(resetToken);
            verify(tokenService).revokeAllUserTokens(testUser);
        }

        @Test
        @DisplayName("Should throw exception for invalid reset token")
        void shouldThrowExceptionForInvalidResetToken() {
            // Given
            ResetPasswordRequest request = new ResetPasswordRequest("invalid-token", "new-password");

            when(tokenService.findValidToken(request.token())).thenReturn(Optional.empty());

            // When & Then
            ValidationException exception = assertThrows(ValidationException.class,
                    () -> authService.resetPassword(request));

            assertEquals("Invalid or expired reset token", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for wrong token type")
        void shouldThrowExceptionForWrongTokenTypeInReset() {
            // Given
            ResetPasswordRequest request = new ResetPasswordRequest("access-token", "new-password");
            testAccessToken.setType(TokenType.ACCESS_TOKEN);

            when(tokenService.findValidToken(request.token())).thenReturn(Optional.of(testAccessToken));

            // When & Then
            ValidationException exception = assertThrows(ValidationException.class,
                    () -> authService.resetPassword(request));

            assertEquals("Invalid token type", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Email Verification Tests")
    class EmailVerificationTests {

        @Test
        @DisplayName("Should verify email successfully")
        void shouldVerifyEmailSuccessfully() {
            // Given
            String tokenValue = "verification-token";
            Token verificationToken = new Token();
            verificationToken.setType(TokenType.EMAIL_VERIFICATION);
            verificationToken.setUser(testUser);
            testUser.setIsEmailVerified(false);

            when(tokenService.findValidToken(tokenValue)).thenReturn(Optional.of(verificationToken));
            when(userRepository.save(testUser)).thenReturn(testUser);

            // When
            authService.verifyEmail(tokenValue);

            // Then
            assertTrue(testUser.getIsEmailVerified());
            verify(userRepository).save(testUser);
            verify(tokenService).markTokenAsUsed(verificationToken);
        }

        @Test
        @DisplayName("Should throw exception for invalid verification token")
        void shouldThrowExceptionForInvalidVerificationToken() {
            // Given
            String tokenValue = "invalid-token";

            when(tokenService.findValidToken(tokenValue)).thenReturn(Optional.empty());

            // When & Then
            ValidationException exception = assertThrows(ValidationException.class,
                    () -> authService.verifyEmail(tokenValue));

            assertEquals("Invalid or expired verification token", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for wrong token type in verification")
        void shouldThrowExceptionForWrongTokenTypeInVerification() {
            // Given
            String tokenValue = "access-token";
            testAccessToken.setType(TokenType.ACCESS_TOKEN);

            when(tokenService.findValidToken(tokenValue)).thenReturn(Optional.of(testAccessToken));

            // When & Then
            ValidationException exception = assertThrows(ValidationException.class,
                    () -> authService.verifyEmail(tokenValue));

            assertEquals("Invalid token type", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Resend Email Verification Tests")
    class ResendEmailVerificationTests {

        @Test
        @DisplayName("Should resend email verification successfully")
        void shouldResendEmailVerificationSuccessfully() {
            // Given
            String email = "test@example.com";
            testUser.setIsEmailVerified(false);
            Token verificationToken = new Token();
            verificationToken.setType(TokenType.EMAIL_VERIFICATION);

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
            when(tokenService.generateEmailVerificationToken(testUser)).thenReturn(verificationToken);

            // When
            authService.resendEmailVerification(email);

            // Then
            verify(tokenService).generateEmailVerificationToken(testUser);
        }

        @Test
        @DisplayName("Should throw exception for non-existent user")
        void shouldThrowExceptionForNonExistentUser() {
            // Given
            String email = "nonexistent@example.com";

            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            // When & Then
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> authService.resendEmailVerification(email));

            assertEquals("User not found", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception for already verified email")
        void shouldThrowExceptionForAlreadyVerifiedEmail() {
            // Given
            String email = "test@example.com";
            testUser.setIsEmailVerified(true);

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

            // When & Then
            ValidationException exception = assertThrows(ValidationException.class,
                    () -> authService.resendEmailVerification(email));

            assertEquals("Email already verified", exception.getMessage());
        }
    }
}
