package com.lagab.eventz.app.domain.auth.service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.lagab.eventz.app.domain.org.dto.OrganizationDto;
import com.lagab.eventz.app.domain.user.model.User;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * EmailService handles sending application emails including :
 * - Account verification emails
 * - Password reset emails
 * - Localized email content based on user preferences
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MessageSource messageSource;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.logo.url}")
    private String logoUrl;

    @Value("${spring.mail.from}")
    private String fromEmail;

    /**
     * Extracts the base language code from a locale string
     *
     * @param localeString Full locale string (e.g., 'en-US' or 'fr_FR')
     * @return The base language code (e.g., 'en' or 'fr')
     */
    private String extractLanguageFromLocale(String localeString) {
        if (localeString == null || localeString.trim().isEmpty()) {
            return "en";
        }
        return localeString.split("[-_]")[0].toLowerCase();
    }

    /**
     * Creates a Locale object from a locale string
     *
     * @param localeString The locale string
     * @return Locale object
     */
    private Locale createLocale(String localeString) {
        String language = extractLanguageFromLocale(localeString);
        return Locale.forLanguageTag(language);
    }

    /**
     * Sends an account verification email to a user
     *
     * @param user  The user to send the email to
     * @param token Verification token to include in the link
     * @return CompletableFuture<Void>
     */
    @Async
    public CompletableFuture<Void> sendValidationEmail(User user, String token) {
        try {
            Locale locale = createLocale(user.getLocale());
            String validationLink = String.format("%s/verify?token=%s", frontendUrl, token);

            // Prepare template variables
            Map<String, Object> templateModel = new HashMap<>();
            templateModel.put("validationLink", validationLink);
            templateModel.put("name", user.getFullName());
            templateModel.put("logo", logoUrl);

            // Create Thymeleaf context
            Context context = new Context(locale, templateModel);

            // Process email template
            String htmlContent = templateEngine.process("emails/verify_email", context);

            // Get localized subject
            String subject = messageSource.getMessage("emails.email_validation.title", null, locale);

            // Send email
            sendHtmlEmail(user.getEmail(), subject, htmlContent);

            log.info("Validation email sent successfully to: {}", user.getEmail());
            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("Failed to send validation email to: {}", user.getEmail(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Sends a password reset email to a user
     *
     * @param user  The user to send the email to
     * @param token Password reset token to include in the link
     * @return CompletableFuture<Void>
     */
    @Async
    public CompletableFuture<Void> sendPasswordResetEmail(User user, String token) {
        try {
            Locale locale = createLocale(user.getLocale());
            String resetLink = String.format("%s/reset-password?token=%s", frontendUrl, token);

            // Prepare template variables
            Map<String, Object> templateModel = new HashMap<>();
            templateModel.put("resetLink", resetLink);
            templateModel.put("name", user.getFullName());
            templateModel.put("logo", logoUrl);
            templateModel.put("locale", locale.getLanguage());

            // Create Thymeleaf context
            Context context = new Context(locale, templateModel);

            // Process email template
            String htmlContent = templateEngine.process("emails/reset_password", context);

            // Get localized subject
            String subject = messageSource.getMessage("emails.password_reset.title", null, locale);

            // Send email
            sendHtmlEmail(user.getEmail(), subject, htmlContent);

            log.info("Password reset email sent successfully to: {}", user.getEmail());
            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", user.getEmail(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Sends an HTML email
     *
     * @param to          Recipient email address
     * @param subject     Email subject
     * @param htmlContent HTML content of the email
     * @throws MessagingException if email sending fails
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true indicates HTML content

        mailSender.send(message);
    }

    /**
     * Sends a simple text email (fallback method)
     *
     * @param to      Recipient email address
     * @param subject Email subject
     * @param content Text content of the email
     * @throws MessagingException if email sending fails
     */
    private void sendTextEmail(String to, String subject, String content) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, false); // false indicates plain text

        mailSender.send(message);
    }

    /**
     * Sends an organization invite email to a user
     *
     * @param organization The organization
     * @param email        The invitee to send the email to
     * @param token        Password reset token to include in the link
     * @return CompletableFuture<Void>
     */
    @Async
    public CompletableFuture<Void> sendOrganizationInvitation(OrganizationDto organization, User inviter, String token, String email) {
        try {
            Locale locale = createLocale(inviter.getLocale());
            String acceptanceLink = String.format("%s/invitations/accept?token=%s", frontendUrl, token);

            // Prepare template variables
            Map<String, Object> templateModel = new HashMap<>();
            templateModel.put("acceptanceLink", acceptanceLink);
            templateModel.put("email", email);
            templateModel.put("organization", organization);
            templateModel.put("inviter", inviter.getFullName());
            templateModel.put("logo", logoUrl);
            templateModel.put("inviteeName", null);
            templateModel.put("locale", locale.getLanguage());

            // Create Thymeleaf context
            Context context = new Context(locale, templateModel);

            // Process email template
            String htmlContent = templateEngine.process("emails/organization_invitation", context);

            // Get localized subject
            String subject = messageSource.getMessage("emails.organization_invitation.title", null, locale);

            // Send email
            sendHtmlEmail(email, subject, htmlContent);

            log.debug("Invitation email sent successfully to: {}", email);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to send invitation email to: {}", email, e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
