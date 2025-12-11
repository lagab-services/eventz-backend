package com.lagab.eventz.app.domain.order.dto;

import java.util.List;

import com.lagab.eventz.app.domain.ticket.dto.AttendeeInfo;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Record representing the data required to create an order
 */
public record OrderRequest(

        // Mandatory billing information
        @NotBlank(message = "Billing name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String billingName,

        @NotBlank(message = "Billing email is required")
        @Email(message = "Invalid email format")
        String billingEmail,

        @Pattern(regexp = "^\\+?[0-9\\s\\-]{8,20}$", message = "Invalid phone number format")
        String billingPhone,

        // Billing address (optional for events)
        String billingAddress,
        String billingCity,
        String billingZipCode,
        String billingCountry,

        // Attendee information (optional)
        @NotEmpty(message = "At least one attendee is required")
        List<AttendeeInfo> attendees,

        // Special requests or notes
        @Size(max = 500, message = "Notes cannot exceed 500 characters")
        String notes,

        // Terms and newsletter
        @AssertTrue(message = "You must accept the terms and conditions")
        boolean acceptTerms,

        boolean subscribeNewsletter,

        // return url
        String successUrl,
        String cancelUrl

) {

}
