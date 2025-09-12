package com.lagab.eventz.app.domain.user.service;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lagab.eventz.app.domain.user.model.User;
import com.lagab.eventz.app.domain.user.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private static final Long VALID_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        testUser = new User();
    }

    @Nested
    @DisplayName("findById() - Success Cases")
    class FindByIdSuccessCases {

        @Test
        @DisplayName("Should return user when user exists")
        void shouldReturnUserWhenUserExists() {
            // Given
            when(userRepository.findById(VALID_USER_ID)).thenReturn(Optional.of(testUser));

            // When
            User result = userService.findById(VALID_USER_ID);

            // Then
            assertThat(result)
                    .isNotNull()
                    .isEqualTo(testUser);

            verify(userRepository).findById(VALID_USER_ID);
            verifyNoMoreInteractions(userRepository);
        }

        @ParameterizedTest
        @DisplayName("Should return user for various valid IDs")
        @ValueSource(longs = { 1L, 100L, 999L, Long.MAX_VALUE })
        void shouldReturnUserForVariousValidIds(Long userId) {
            // Given
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            // When
            User result = userService.findById(userId);

            // Then
            assertThat(result).isEqualTo(testUser);
            verify(userRepository).findById(userId);
        }
    }

    @Nested
    @DisplayName("findById() - Error Cases")
    class FindByIdErrorCases {

        @ParameterizedTest
        @DisplayName("Should throw EntityNotFoundException for non-existent users")
        @MethodSource("provideNonExistentUserIds")
        void shouldThrowEntityNotFoundExceptionForNonExistentUsers(Long userId, String expectedMessage) {
            // Given
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.findById(userId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(expectedMessage);

            verify(userRepository).findById(userId);
        }

        @ParameterizedTest
        @DisplayName("Should throw EntityNotFoundException for edge case IDs")
        @NullSource
        @ValueSource(longs = { 0L, -1L, -999L })
        void shouldThrowEntityNotFoundExceptionForEdgeCaseIds(Long userId) {
            // Given
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.findById(userId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("User not found with id:" + userId);

            verify(userRepository).findById(userId);
        }

        @Test
        @DisplayName("Should propagate repository exceptions")
        void shouldPropagateRepositoryExceptions() {
            // Given
            RuntimeException repositoryException = new RuntimeException("Database connection error");
            when(userRepository.findById(VALID_USER_ID)).thenThrow(repositoryException);

            // When & Then
            assertThatThrownBy(() -> userService.findById(VALID_USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database connection error");

            verify(userRepository).findById(VALID_USER_ID);
        }

        private static Stream<Arguments> provideNonExistentUserIds() {
            return Stream.of(
                    Arguments.of(999L, "User not found with id:999"),
                    Arguments.of(12345L, "User not found with id:12345"),
                    Arguments.of(Long.MAX_VALUE - 1, "User not found with id:" + (Long.MAX_VALUE - 1))
            );
        }
    }

    @Nested
    @DisplayName("Repository Interaction Tests")
    class RepositoryInteractionTests {

        @Test
        @DisplayName("Should call repository exactly once for successful lookup")
        void shouldCallRepositoryExactlyOnceForSuccessfulLookup() {
            // Given
            when(userRepository.findById(VALID_USER_ID)).thenReturn(Optional.of(testUser));

            // When
            userService.findById(VALID_USER_ID);

            // Then
            verify(userRepository, times(1)).findById(VALID_USER_ID);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("Should call repository exactly once for failed lookup")
        void shouldCallRepositoryExactlyOnceForFailedLookup() {
            // Given
            Long nonExistentId = 999L;
            when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.findById(nonExistentId))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(userRepository, times(1)).findById(nonExistentId);
            verifyNoMoreInteractions(userRepository);
        }
    }
}
