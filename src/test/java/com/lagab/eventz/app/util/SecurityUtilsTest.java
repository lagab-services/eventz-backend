package com.lagab.eventz.app.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.lagab.eventz.app.common.exception.UnauthorizedException;
import com.lagab.eventz.app.domain.user.model.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityUtils Tests")
class SecurityUtilsTest {

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private User user;

    private MockedStatic<SecurityContextHolder> securityContextHolderMock;

    @BeforeEach
    void setUp() {
        securityContextHolderMock = mockStatic(SecurityContextHolder.class);
        securityContextHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
    }

    @AfterEach
    void tearDown() {
        securityContextHolderMock.close();
    }

    @Nested
    @DisplayName("getCurrentUserId() Tests")
    class GetCurrentUserIdTests {

        @Test
        @DisplayName("Should return user ID when authentication is valid and principal is User")
        void shouldReturnUserIdWhenAuthenticationIsValidAndPrincipalIsUser() {
            // Given
            Long expectedUserId = 123L;
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(user);
            when(user.getId()).thenReturn(expectedUserId);

            // When
            Long actualUserId = SecurityUtils.getCurrentUserId();

            // Then
            assertEquals(expectedUserId, actualUserId);
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when authentication is null")
        void shouldThrowUnauthorizedExceptionWhenAuthenticationIsNull() {
            // Given
            when(securityContext.getAuthentication()).thenReturn(null);

            // When & Then
            UnauthorizedException exception = assertThrows(
                    UnauthorizedException.class,
                    SecurityUtils::getCurrentUserId
            );
            assertEquals("User not authenticated", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when user is not authenticated")
        void shouldThrowUnauthorizedExceptionWhenUserIsNotAuthenticated() {
            // Given
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(false);

            // When & Then
            UnauthorizedException exception = assertThrows(
                    UnauthorizedException.class,
                    SecurityUtils::getCurrentUserId
            );
            assertEquals("User not authenticated", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when principal is not User instance")
        void shouldThrowUnauthorizedExceptionWhenPrincipalIsNotUserInstance() {
            // Given
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn("not-a-user-object");

            // When & Then
            UnauthorizedException exception = assertThrows(
                    UnauthorizedException.class,
                    SecurityUtils::getCurrentUserId
            );
            assertEquals("Unable to extract user ID from authentication", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("getCurrentUserIdOrNull() Tests")
    class GetCurrentUserIdOrNullTests {

        @Test
        @DisplayName("Should return user ID when authentication is valid")
        void shouldReturnUserIdWhenAuthenticationIsValid() {
            // Given
            Long expectedUserId = 123L;
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(user);
            when(user.getId()).thenReturn(expectedUserId);

            // When
            Long actualUserId = SecurityUtils.getCurrentUserIdOrNull();

            // Then
            assertEquals(expectedUserId, actualUserId);
        }

        @Test
        @DisplayName("Should return null when authentication fails")
        void shouldReturnNullWhenAuthenticationFails() {
            // Given
            when(securityContext.getAuthentication()).thenReturn(null);

            // When
            Long actualUserId = SecurityUtils.getCurrentUserIdOrNull();

            // Then
            assertNull(actualUserId);
        }

        @Test
        @DisplayName("Should return null when principal is not User instance")
        void shouldReturnNullWhenPrincipalIsNotUserInstance() {
            // Given
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn("not-a-user-object");

            // When
            Long actualUserId = SecurityUtils.getCurrentUserIdOrNull();

            // Then
            assertNull(actualUserId);
        }
    }

    @Nested
    @DisplayName("getCurrentUserEmail() Tests")
    class GetCurrentUserEmailTests {

        @Test
        @DisplayName("Should return user email when authentication is valid and principal is User")
        void shouldReturnUserEmailWhenAuthenticationIsValidAndPrincipalIsUser() {
            // Given
            String expectedEmail = "test@example.com";
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(user);
            when(user.getEmail()).thenReturn(expectedEmail);

            // When
            String actualEmail = SecurityUtils.getCurrentUserEmail();

            // Then
            assertEquals(expectedEmail, actualEmail);
        }

        @Test
        @DisplayName("Should return email from JWT details when principal is not User")
        void shouldReturnEmailFromJwtDetailsWhenPrincipalIsNotUser() {
            // Given
            String expectedEmail = "jwt@example.com";
            Map<String, Object> details = new HashMap<>();
            details.put("email", expectedEmail);

            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn("jwt-principal");
            when(authentication.getDetails()).thenReturn(details);

            // When
            String actualEmail = SecurityUtils.getCurrentUserEmail();

            // Then
            assertEquals(expectedEmail, actualEmail);
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when authentication is null")
        void shouldThrowUnauthorizedExceptionWhenAuthenticationIsNull() {
            // Given
            when(securityContext.getAuthentication()).thenReturn(null);

            // When & Then
            UnauthorizedException exception = assertThrows(
                    UnauthorizedException.class,
                    SecurityUtils::getCurrentUserEmail
            );
            assertEquals("User not authenticated", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when user is not authenticated")
        void shouldThrowUnauthorizedExceptionWhenUserIsNotAuthenticated() {
            // Given
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(false);

            // When & Then
            UnauthorizedException exception = assertThrows(
                    UnauthorizedException.class,
                    SecurityUtils::getCurrentUserEmail
            );
            assertEquals("User not authenticated", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when unable to extract email")
        void shouldThrowUnauthorizedExceptionWhenUnableToExtractEmail() {
            // Given
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn("not-a-user-object");
            when(authentication.getDetails()).thenReturn("not-a-map");

            // When & Then
            UnauthorizedException exception = assertThrows(
                    UnauthorizedException.class,
                    SecurityUtils::getCurrentUserEmail
            );
            assertEquals("Unable to extract user email from authentication", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("getCurrentUser() Tests")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Should return current user when authentication is valid and principal is User")
        void shouldReturnCurrentUserWhenAuthenticationIsValidAndPrincipalIsUser() {
            // Given
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(user);

            // When
            User actualUser = SecurityUtils.getCurrentUser();

            // Then
            assertEquals(user, actualUser);
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when authentication is null")
        void shouldThrowUnauthorizedExceptionWhenAuthenticationIsNull() {
            // Given
            when(securityContext.getAuthentication()).thenReturn(null);

            // When & Then
            UnauthorizedException exception = assertThrows(
                    UnauthorizedException.class,
                    SecurityUtils::getCurrentUser
            );
            assertEquals("User not authenticated", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when user is not authenticated")
        void shouldThrowUnauthorizedExceptionWhenUserIsNotAuthenticated() {
            // Given
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(false);

            // When & Then
            UnauthorizedException exception = assertThrows(
                    UnauthorizedException.class,
                    SecurityUtils::getCurrentUser
            );
            assertEquals("User not authenticated", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when principal is not User instance")
        void shouldThrowUnauthorizedExceptionWhenPrincipalIsNotUserInstance() {
            // Given
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn("not-a-user-object");

            // When & Then
            UnauthorizedException exception = assertThrows(
                    UnauthorizedException.class,
                    SecurityUtils::getCurrentUser
            );
            assertEquals("Unable to extract user ID from authentication", exception.getMessage());
        }
    }
}
