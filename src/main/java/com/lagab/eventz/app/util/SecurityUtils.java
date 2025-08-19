package com.lagab.eventz.app.util;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.lagab.eventz.app.common.exception.UnauthorizedException;
import com.lagab.eventz.app.domain.user.model.User;

@Component

public class SecurityUtils {

    /**
     * Extract user ID from Spring Security Authentication
     *
     * @return Current user ID
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }
        if (authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getId();
        }
        throw new UnauthorizedException("Unable to extract user ID from authentication");
    }

    /**
     * Extract user ID from Spring Security Authentication
     *
     * @return Current user ID or null if error
     */
    public static Long getCurrentUserIdOrNull() {
        try {
            return getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract user email from Spring Security Authentication
     *
     * @return Current user email
     */
    public static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof User userPrincipal) {
            return userPrincipal.getEmail();
        }

        // If using JWT with email claim
        if (authentication.getDetails() instanceof Map<?, ?> details) {
            return (String) details.get("email");
        }

        throw new UnauthorizedException("Unable to extract user email from authentication");
    }

    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }
        if (authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal());
        }
        throw new UnauthorizedException("Unable to extract user ID from authentication");
    }
}
