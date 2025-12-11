package com.lagab.eventz.app.infrastructure.security;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.lagab.eventz.app.domain.auth.service.JwtService;
import com.lagab.eventz.app.domain.user.model.Role;
import com.lagab.eventz.app.domain.user.model.User;
import com.lagab.eventz.app.domain.user.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Authentication Filter Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String BEARER_TOKEN = "Bearer " + VALID_TOKEN;
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Valid Authentication Tests")
    class ValidAuthenticationTests {

        @ParameterizedTest
        @EnumSource(Role.class)
        @DisplayName("Should set authentication for enabled users with different roles")
        void doFilterInternal_WithValidTokenAndEnabledUser_ShouldSetAuthentication(Role role) throws ServletException, IOException {
            // Given
            User user = createUserWithRole(role);
            when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
            when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
            when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
                mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
                when(securityContext.getAuthentication()).thenReturn(null);

                // When
                jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

                // Then
                ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor =
                        ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
                verify(securityContext).setAuthentication(authCaptor.capture());

                UsernamePasswordAuthenticationToken capturedAuth = authCaptor.getValue();
                assertEquals(user, capturedAuth.getPrincipal());
                assertNull(capturedAuth.getCredentials());
                assertEquals(1, capturedAuth.getAuthorities().size());
                assertTrue(capturedAuth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_" + role.name())));
                assertNotNull(capturedAuth.getDetails());

                verify(filterChain).doFilter(request, response);
            }
        }

        private User createUserWithRole(Role role) {
            return User.builder()
                       .id(USER_ID)
                       .email(role.name().toLowerCase() + "@example.com")
                       .password("password")
                       .firstName("Test")
                       .lastName("User")
                       .role(role)
                       .isActive(true)
                       .isEmailVerified(true)
                       .createdAt(LocalDateTime.now())
                       .updatedAt(LocalDateTime.now())
                       .build();
        }
    }

    @Nested
    @DisplayName("Disabled User Tests")
    class DisabledUserTests {

        @ParameterizedTest
        @MethodSource("disabledUserScenarios")
        @DisplayName("Should not set authentication for disabled users")
        void doFilterInternal_WithDisabledUser_ShouldNotSetAuthentication(
                String scenario, boolean isActive, boolean isEmailVerified) throws ServletException, IOException {

            // Given
            User disabledUser = User.builder()
                                    .id(USER_ID)
                                    .email("disabled@example.com")
                                    .password("password")
                                    .firstName("Disabled")
                                    .lastName("User")
                                    .role(Role.USER)
                                    .isActive(isActive)
                                    .isEmailVerified(isEmailVerified)
                                    .createdAt(LocalDateTime.now())
                                    .updatedAt(LocalDateTime.now())
                                    .build();

            when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
            when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
            when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(disabledUser));

            try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
                mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
                when(securityContext.getAuthentication()).thenReturn(null);

                // When
                jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

                // Then
                verify(securityContext, never()).setAuthentication(any());
                assertFalse(disabledUser.isEnabled(), "User should be disabled for scenario: " + scenario);
                verify(filterChain).doFilter(request, response);
            }
        }

        static Stream<Arguments> disabledUserScenarios() {
            return Stream.of(
                    Arguments.of("Inactive user", false, true),
                    Arguments.of("Unverified user", true, false),
                    Arguments.of("Completely disabled user", false, false)
            );
        }
    }

    @Nested
    @DisplayName("Invalid Authorization Header Tests")
    class InvalidAuthorizationHeaderTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "",                           // Empty string
                "   ",                       // Whitespace only
                "Basic dXNlcjpwYXNz",        // Basic auth
                "bearer token123",           // Lowercase bearer
                "Bearer",                    // Bearer without token
                "Bearer ",                   // Bearer with space only
                "InvalidFormat token"        // Invalid format
        })
        @DisplayName("Should not set authentication for invalid authorization headers")
        void doFilterInternal_WithInvalidAuthorizationHeader_ShouldNotSetAuthentication(String authHeader)
                throws ServletException, IOException {

            // Given
            when(request.getHeader("Authorization")).thenReturn(authHeader);

            try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
                mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                // When
                jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

                // Then
                verify(jwtService, never()).isTokenValid(any());
                verify(securityContext, never()).setAuthentication(any());
                verify(filterChain).doFilter(request, response);
            }
        }

        @Test
        @DisplayName("Should not set authentication when authorization header is null")
        void doFilterInternal_WithNullAuthorizationHeader_ShouldNotSetAuthentication() throws ServletException, IOException {
            // Given
            when(request.getHeader("Authorization")).thenReturn(null);

            try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
                mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                // When
                jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

                // Then
                verify(securityContext, never()).setAuthentication(any());
                verify(jwtService, never()).isTokenValid(any());
                verify(filterChain).doFilter(request, response);
            }
        }
    }

    @Nested
    @DisplayName("Exception Handling Tests")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("Should continue filter chain when JWT service throws exception")
        void doFilterInternal_WithJwtServiceException_ShouldContinueFilterChain() throws ServletException, IOException {
            // Given
            when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
            when(jwtService.isTokenValid(VALID_TOKEN)).thenThrow(new RuntimeException("JWT parsing error"));

            try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
                mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
                when(securityContext.getAuthentication()).thenReturn(null);

                // When
                jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

                // Then
                verify(securityContext, never()).setAuthentication(any());
                verify(jwtService).isTokenValid(VALID_TOKEN);
                verify(filterChain).doFilter(request, response);
            }
        }

        @Test
        @DisplayName("Should continue filter chain when user repository throws exception")
        void doFilterInternal_WithUserRepositoryException_ShouldContinueFilterChain() throws ServletException, IOException {
            // Given
            when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
            when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
            when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(USER_ID);
            when(userRepository.findById(USER_ID)).thenThrow(new RuntimeException("Database error"));

            try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
                mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
                when(securityContext.getAuthentication()).thenReturn(null);

                // When
                jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

                // Then
                verify(securityContext, never()).setAuthentication(any());
                verify(jwtService).isTokenValid(VALID_TOKEN);
                verify(jwtService).extractUserId(VALID_TOKEN);
                verify(userRepository).findById(USER_ID);
                verify(filterChain).doFilter(request, response);
            }
        }

        @Test
        @DisplayName("Should continue filter chain when extractUserId throws exception")
        void doFilterInternal_WithExtractUserIdException_ShouldContinueFilterChain() throws ServletException, IOException {
            // Given
            when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
            when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
            when(jwtService.extractUserId(VALID_TOKEN)).thenThrow(new RuntimeException("Token parsing error"));

            try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
                mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
                when(securityContext.getAuthentication()).thenReturn(null);

                // When
                jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

                // Then
                verify(securityContext, never()).setAuthentication(any());
                verify(jwtService).isTokenValid(VALID_TOKEN);
                verify(jwtService).extractUserId(VALID_TOKEN);
                verify(filterChain).doFilter(request, response);
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should not set authentication when token is invalid")
        void doFilterInternal_WithInvalidToken_ShouldNotSetAuthentication() throws ServletException, IOException {
            // Given
            when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
            when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(false);

            try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
                mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
                when(securityContext.getAuthentication()).thenReturn(null);

                // When
                jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

                // Then
                verify(securityContext, never()).setAuthentication(any());
                verify(jwtService).isTokenValid(VALID_TOKEN);
                verify(jwtService, never()).extractUserId(any());
                verify(filterChain).doFilter(request, response);
            }
        }

        @Test
        @DisplayName("Should not override existing authentication")
        void doFilterInternal_WithExistingAuthentication_ShouldNotOverrideAuthentication() throws ServletException, IOException {
            // Given
            when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
            Authentication existingAuth = mock(Authentication.class);

            try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
                mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
                when(securityContext.getAuthentication()).thenReturn(existingAuth);

                // When
                jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

                // Then
                verify(jwtService, never()).isTokenValid(any());
                verify(securityContext, never()).setAuthentication(any());
                verify(filterChain).doFilter(request, response);
            }
        }

        @Test
        @DisplayName("Should not set authentication when user not found")
        void doFilterInternal_WithUserNotFound_ShouldNotSetAuthentication() throws ServletException, IOException {
            // Given
            when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
            when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
            when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(USER_ID);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
                mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
                when(securityContext.getAuthentication()).thenReturn(null);

                // When
                jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

                // Then
                verify(securityContext, never()).setAuthentication(any());
                verify(jwtService).isTokenValid(VALID_TOKEN);
                verify(jwtService).extractUserId(VALID_TOKEN);
                verify(userRepository).findById(USER_ID);
                verify(filterChain).doFilter(request, response);
            }
        }

        @Test
        @DisplayName("Should not set authentication when user ID is null")
        void doFilterInternal_WithNullUserId_ShouldNotSetAuthentication() throws ServletException, IOException {
            // Given
            when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
            when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
            when(jwtService.extractUserId(VALID_TOKEN)).thenReturn(null);

            try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
                mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
                when(securityContext.getAuthentication()).thenReturn(null);

                // When
                jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

                // Then
                verify(securityContext, never()).setAuthentication(any());
                verify(filterChain).doFilter(request, response);
            }
        }

        @Test
        @DisplayName("Should preserve extra spaces in token extraction")
        void doFilterInternal_WithBearerTokenWithExtraSpaces_ShouldExtractCorrectToken() throws ServletException, IOException {
            // Given
            String tokenWithSpaces = "Bearer   " + VALID_TOKEN;
            when(request.getHeader("Authorization")).thenReturn(tokenWithSpaces);
            when(jwtService.isTokenValid("  " + VALID_TOKEN)).thenReturn(false); // Should fail because of extra spaces

            try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
                mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
                when(securityContext.getAuthentication()).thenReturn(null);

                // When
                jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

                // Then
                verify(jwtService).isTokenValid("  " + VALID_TOKEN); // Verifies that extra spaces are preserved
                verify(securityContext, never()).setAuthentication(any());
                verify(filterChain).doFilter(request, response);
            }
        }

        @Test
        @DisplayName("Should always call filter chain even with exceptions")
        void doFilterInternal_AlwaysCallsFilterChain_EvenWithExceptions() throws ServletException, IOException {
            // Given
            when(request.getHeader("Authorization")).thenReturn(BEARER_TOKEN);
            when(jwtService.isTokenValid(VALID_TOKEN)).thenThrow(new RuntimeException("Any exception"));

            try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class)) {
                mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
                when(securityContext.getAuthentication()).thenReturn(null);

                // When
                jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

                // Then
                verify(filterChain).doFilter(request, response);
            }
        }
    }

    @Nested
    @DisplayName("Protected Method Tests")
    class ProtectedMethodTests {

        @ParameterizedTest
        @MethodSource("tokenExtractionScenarios")
        @DisplayName("Should extract token correctly from various authorization headers")
        void extractTokenFromRequest_ShouldHandleVariousScenarios(String authHeader, String expectedToken)
                throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            // Given
            when(request.getHeader("Authorization")).thenReturn(authHeader);

            // When - Using reflection to access private method
            Method extractTokenMethod = JwtAuthenticationFilter.class.getDeclaredMethod("extractTokenFromRequest", HttpServletRequest.class);
            extractTokenMethod.setAccessible(true);
            String result = (String) extractTokenMethod.invoke(jwtAuthenticationFilter, request);

            // Then
            assertEquals(expectedToken, result);
        }

        static Stream<Arguments> tokenExtractionScenarios() {
            return Stream.of(
                    Arguments.of("Bearer validToken123", "validToken123"),
                    Arguments.of("Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.signature", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.signature"),
                    Arguments.of("Bearer   tokenWithSpaces", "  tokenWithSpaces"),
                    Arguments.of(null, null),
                    Arguments.of("", null),
                    Arguments.of("   ", null),
                    Arguments.of("Basic dXNlcjpwYXNz", null),
                    Arguments.of("bearer lowercase", null),
                    Arguments.of("Bearer", null),
                    Arguments.of("InvalidFormat token", null)
            );
        }
    }
}

