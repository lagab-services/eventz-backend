package com.lagab.eventz.app.domain.auth.service;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.lagab.eventz.app.domain.user.model.User;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private MessageSource messageSource;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    private User testUser;
    private static final String TEST_TOKEN = "test-token-123";
    private static final String FRONTEND_URL = "https://frontend.test.com";
    private static final String LOGO_URL = "https://logo.test.com/logo.png";
    private static final String FROM_EMAIL = "noreply@test.com";

    @BeforeEach
    void setUp() {
        // Configure properties via ReflectionTestUtils
        ReflectionTestUtils.setField(emailService, "frontendUrl", FRONTEND_URL);
        ReflectionTestUtils.setField(emailService, "logoUrl", LOGO_URL);
        ReflectionTestUtils.setField(emailService, "fromEmail", FROM_EMAIL);

        // Create a test user
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setLocale("en-US");
    }

    @Test
    void testExtractLanguageFromLocale_WithEnglishLocale() {
        // Test with standard English locale
        String result = invokePrivateMethod("extractLanguageFromLocale", "en-US");
        assertEquals("en", result);
    }

    @Test
    void testExtractLanguageFromLocale_WithFrenchLocale() {
        // Test with French locale
        String result = invokePrivateMethod("extractLanguageFromLocale", "fr-FR");
        assertEquals("fr", result);
    }

    @Test
    void testExtractLanguageFromLocale_WithUnderscoreSeparator() {
        // Test with underscore separator
        String result = invokePrivateMethod("extractLanguageFromLocale", "de_DE");
        assertEquals("de", result);
    }

    @Test
    void testExtractLanguageFromLocale_WithNullLocale() {
        // Test with null locale
        String result = invokePrivateMethod("extractLanguageFromLocale", (String) null);
        assertEquals("en", result);
    }

    @Test
    void testExtractLanguageFromLocale_WithEmptyLocale() {
        // Test with empty locale
        String result = invokePrivateMethod("extractLanguageFromLocale", "");
        assertEquals("en", result);
    }

    @Test
    void testExtractLanguageFromLocale_WithWhitespaceLocale() {
        // Test with locale containing only whitespace
        String result = invokePrivateMethod("extractLanguageFromLocale", "   ");
        assertEquals("en", result);
    }

    @Test
    void testCreateLocale_WithValidLocale() {
        // Test Locale creation with valid string
        Locale result = invokePrivateMethod("createLocale", "fr-FR");
        assertEquals("fr", result.getLanguage());
    }

    @Test
    void testCreateLocale_WithNullLocale() {
        // Test Locale creation with null
        Locale result = invokePrivateMethod("createLocale", (String) null);
        assertEquals("en", result.getLanguage());
    }

    @Test
    void testSendValidationEmail_Success() throws Exception {
        // Mock configuration
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("emails/verify_email"), any(Context.class)))
                .thenReturn("<html>Test HTML Content</html>");
        when(messageSource.getMessage(eq("emails.email_validation.title"), isNull(), any(Locale.class)))
                .thenReturn("Verify Your Email");

        // Execution
        CompletableFuture<Void> result = emailService.sendValidationEmail(testUser, TEST_TOKEN);

        // Verifications
        assertNotNull(result);
        assertTrue(result.isDone());
        assertFalse(result.isCompletedExceptionally());

        // Verify interactions
        verify(mailSender).createMimeMessage();
        verify(templateEngine).process(eq("emails/verify_email"), any(Context.class));
        verify(messageSource).getMessage(eq("emails.email_validation.title"), isNull(), any(Locale.class));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void testSendValidationEmail_WithFrenchLocale() throws Exception {
        // Configuration with French locale
        testUser.setLocale("fr-FR");

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("emails/verify_email"), any(Context.class)))
                .thenReturn("<html>Test HTML Content</html>");
        when(messageSource.getMessage(eq("emails.email_validation.title"), isNull(), any(Locale.class)))
                .thenReturn("Vérifiez votre email");

        // Execution
        CompletableFuture<Void> result = emailService.sendValidationEmail(testUser, TEST_TOKEN);

        // Verifications
        assertNotNull(result);
        assertTrue(result.isDone());
        assertFalse(result.isCompletedExceptionally());

        // Verify that context contains correct data
        verify(templateEngine).process(eq("emails/verify_email"), argThat(context -> {
            return context.getLocale().getLanguage().equals("fr");
        }));
    }

    @Test
    void testSendPasswordResetEmail_Success() throws Exception {
        // Mock configuration
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("emails/reset_password"), any(Context.class)))
                .thenReturn("<html>Password Reset HTML</html>");
        when(messageSource.getMessage(eq("emails.password_reset.title"), isNull(), any(Locale.class)))
                .thenReturn("Reset Your Password");

        // Execution
        CompletableFuture<Void> result = emailService.sendPasswordResetEmail(testUser, TEST_TOKEN);

        // Verifications
        assertNotNull(result);
        assertTrue(result.isDone());
        assertFalse(result.isCompletedExceptionally());

        // Verify interactions
        verify(mailSender).createMimeMessage();
        verify(templateEngine).process(eq("emails/reset_password"), any(Context.class));
        verify(messageSource).getMessage(eq("emails.password_reset.title"), isNull(), any(Locale.class));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void testSendPasswordResetEmail_WithDifferentLocale() throws Exception {
        // Configuration with Spanish locale
        testUser.setLocale("es-ES");

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("emails/reset_password"), any(Context.class)))
                .thenReturn("<html>Restablecer Contraseña</html>");
        when(messageSource.getMessage(eq("emails.password_reset.title"), isNull(), any(Locale.class)))
                .thenReturn("Restablece tu contraseña");

        // Execution
        CompletableFuture<Void> result = emailService.sendPasswordResetEmail(testUser, TEST_TOKEN);

        // Verifications
        assertNotNull(result);
        assertTrue(result.isDone());

        // Verify that context uses correct locale
        verify(templateEngine).process(eq("emails/reset_password"), argThat(context -> {
            return context.getLocale().getLanguage().equals("es");
        }));
    }

    @Test
    void testSendPasswordResetEmail_Failure() throws Exception {
        // Configuration to simulate exception during sending
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(any(String.class), any(Context.class)))
                .thenReturn("<html>Test</html>");
        when(messageSource.getMessage(any(String.class), any(), any(Locale.class)))
                .thenReturn("Test Subject");
        doThrow(new RuntimeException("Mail sending failed")).when(mailSender).send(any(MimeMessage.class));

        // Execution
        CompletableFuture<Void> result = emailService.sendPasswordResetEmail(testUser, TEST_TOKEN);

        // Verifications
        assertNotNull(result);
        assertTrue(result.isDone());
        assertTrue(result.isCompletedExceptionally());
    }

    @Test
    void testSendHtmlEmail_Success() throws Exception {
        // Mock configuration
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Using reflection to test private method
        invokePrivateMethodVoid("sendHtmlEmail", "test@example.com", "Test Subject", "<html>Test Content</html>");

        // Verifications
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void testSendTextEmail_Success() throws Exception {
        // Mock configuration
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Test private method sendTextEmail
        invokePrivateMethodVoid("sendTextEmail", "test@example.com", "Test Subject", "Plain text content");

        // Verifications
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void testTemplateVariables_ValidationEmail() throws Exception {
        // Mock configuration
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("emails/verify_email"), any(Context.class)))
                .thenReturn("<html>Test</html>");
        when(messageSource.getMessage(any(String.class), any(), any(Locale.class)))
                .thenReturn("Test Subject");

        // Execution
        emailService.sendValidationEmail(testUser, TEST_TOKEN);

        // Verify that template receives correct variables
        verify(templateEngine).process(eq("emails/verify_email"), argThat(context -> {
            Object validationLink = context.getVariable("validationLink");
            Object name = context.getVariable("name");
            Object logo = context.getVariable("logo");

            return validationLink != null &&
                    validationLink.toString().contains(FRONTEND_URL) &&
                    validationLink.toString().contains(TEST_TOKEN) &&
                    name != null &&
                    logo != null;
        }));
    }

    @Test
    void testTemplateVariables_PasswordResetEmail() throws Exception {
        // Mock configuration
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("emails/reset_password"), any(Context.class)))
                .thenReturn("<html>Test</html>");
        when(messageSource.getMessage(any(String.class), any(), any(Locale.class)))
                .thenReturn("Test Subject");

        // Execution
        emailService.sendPasswordResetEmail(testUser, TEST_TOKEN);

        // Verify that template receives correct variables
        verify(templateEngine).process(eq("emails/reset_password"), argThat(context -> {
            Object resetLink = context.getVariable("resetLink");
            Object name = context.getVariable("name");
            Object logo = context.getVariable("logo");
            Object locale = context.getVariable("locale");

            return resetLink != null &&
                    resetLink.toString().contains(FRONTEND_URL) &&
                    resetLink.toString().contains(TEST_TOKEN) &&
                    name != null &&
                    logo != null &&
                    locale != null;
        }));
    }

    // Utility methods to invoke private methods
    @SuppressWarnings("unchecked")
    private <T> T invokePrivateMethod(String methodName, Object... args) {
        try {
            Class<?>[] paramTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] == null) {
                    paramTypes[i] = String.class; // Assumption for tests
                } else {
                    paramTypes[i] = args[i].getClass();
                }
            }

            var method = EmailService.class.getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return (T) method.invoke(emailService, args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke private method: " + methodName, e);
        }
    }

    private void invokePrivateMethodVoid(String methodName, Object... args) {
        try {
            Class<?>[] paramTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i].getClass();
            }

            var method = EmailService.class.getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            method.invoke(emailService, args);
        } catch (Exception e) {
            if (e.getCause() instanceof MessagingException) {
                throw new RuntimeException(e.getCause());
            }
            throw new RuntimeException("Failed to invoke private method: " + methodName, e);
        }
    }
}
