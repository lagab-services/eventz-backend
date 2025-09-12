package com.lagab.eventz.app.common.exception;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.lagab.eventz.app.common.dto.MessageResponse;

import jakarta.persistence.EntityNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private MethodArgumentNotValidException methodArgumentNotValidException;

    @BeforeEach
    void setUp() {
        // Configuration commune si nécessaire
    }

    @Test
    void handleAuthenticationException_ShouldReturnUnauthorized() {
        // Given
        String errorMessage = "Authentication failed";
        AuthenticationException exception = new AuthenticationException(errorMessage);

        // When
        ResponseEntity<MessageResponse> response = globalExceptionHandler.handleAuthenticationException(exception);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(errorMessage, response.getBody().message());
    }

    @Test
    void handleDisabledException_ShouldReturnUnauthorized() {
        // Given
        String errorMessage = "Account is disabled";
        DisabledException exception = new DisabledException(errorMessage);

        // When
        ResponseEntity<MessageResponse> response = globalExceptionHandler.handleDisabledException(exception);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(errorMessage, response.getBody().message());
    }

    @Test
    void handleValidationException_ShouldReturnBadRequest() {
        // Given
        String errorMessage = "Validation failed";
        ValidationException exception = new ValidationException(errorMessage);

        // When
        ResponseEntity<MessageResponse> response = globalExceptionHandler.handleValidationException(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(errorMessage, response.getBody().message());
    }

    @Test
    void handleResourceNotFoundException_ShouldReturnNotFound() {
        // Given
        String errorMessage = "Resource not found";
        ResourceNotFoundException exception = new ResourceNotFoundException(errorMessage);

        // When
        ResponseEntity<MessageResponse> response = globalExceptionHandler.handleResourceNotFoundException(exception);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(errorMessage, response.getBody().message());
    }

    @Test
    void handleEntityNotFound_ShouldReturnNotFound() {
        // Given
        String errorMessage = "Entity not found";
        EntityNotFoundException exception = new EntityNotFoundException(errorMessage);

        // When
        ResponseEntity<MessageResponse> response = globalExceptionHandler.handleEntityNotFound(exception);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(errorMessage, response.getBody().message());
    }

    @Test
    void handleHandlerNotFound_ShouldReturnNotFound() {
        // Given
        String errorMessage = "No endpoint GET";
        NoHandlerFoundException exception = new NoHandlerFoundException("GET", "/test", null);

        // When
        ResponseEntity<MessageResponse> response = globalExceptionHandler.handleHandlerNotFound(exception);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().message().contains(errorMessage));
    }

    @Test
    void handleAccessDeniedException_ShouldReturnForbidden() {
        // Given
        String errorMessage = "Access denied";
        AccessDeniedException exception = new AccessDeniedException(errorMessage);

        // When
        ResponseEntity<MessageResponse> response = globalExceptionHandler.handleAccessDeniedException(exception);

        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Access denied", response.getBody().message());
    }

    @Test
    void handleValidationExceptions_ShouldReturnBadRequestWithFieldErrors() {
        // Given
        FieldError fieldError1 = new FieldError("objectName", "field1", "Field1 is required");
        FieldError fieldError2 = new FieldError("objectName", "field2", "Field2 must be valid");

        when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError1, fieldError2));

        // When
        ResponseEntity<Map<String, String>> response = globalExceptionHandler.handleValidationExceptions(methodArgumentNotValidException);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("Field1 is required", response.getBody().get("field1"));
        assertEquals("Field2 must be valid", response.getBody().get("field2"));
    }

    @Test
    void handleGenericException_ShouldReturnInternalServerError() {
        // Given
        Exception exception = new RuntimeException("Unexpected error");

        // When
        ResponseEntity<MessageResponse> response = globalExceptionHandler.handleGenericException(exception);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("An internal error occurred", response.getBody().message());
    }

    @Test
    void handleGenericException_WithNullMessage_ShouldReturnInternalServerError() {
        // Given
        Exception exception = new RuntimeException();

        // When
        ResponseEntity<MessageResponse> response = globalExceptionHandler.handleGenericException(exception);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("An internal error occurred", response.getBody().message());
    }
}
