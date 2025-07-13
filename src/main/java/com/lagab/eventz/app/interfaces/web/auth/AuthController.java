package com.lagab.eventz.app.interfaces.web.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lagab.eventz.app.common.dto.MessageResponse;
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
import com.lagab.eventz.app.interfaces.web.auth.dto.EmailRequest;
import com.lagab.eventz.app.interfaces.web.auth.dto.TokenRequest;
import com.lagab.eventz.app.util.HttpUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and user management endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(
            summary = "User login",
            description = "Authenticates a user with email/username and password. Returns JWT tokens and user information."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "423",
                    description = "Account locked due to too many failed attempts",
                    content = @Content
            )
    })
    public ResponseEntity<AuthResponse> login(
            @Parameter(description = "Login credentials", required = true)
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponse response = authService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(
            summary = "User registration",
            description = "Creates a new user account. Returns JWT tokens and user information upon successful registration."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Registration successful",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters or validation errors",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "User already exists with this email",
                    content = @Content
            )
    })
    public ResponseEntity<AuthResponse> register(
            @Parameter(description = "User registration details", required = true)
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh JWT token",
            description = "Generates a new access token using a valid refresh token. Updates device information if provided."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid or expired refresh token",
                    content = @Content
            )
    })
    public ResponseEntity<AuthResponse> refreshToken(
            @Parameter(description = "Refresh token request", required = true)
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponse response = authService.refreshToken(request, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Logout user",
            description = "Logs out the user by invalidating the current JWT token."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Logout successful",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content
            )
    })
    public ResponseEntity<MessageResponse> logout(HttpServletRequest request) {
        String token = HttpUtil.extractTokenFromRequest(request);
        if (token != null) {
            authService.logout(token);
        }
        return ResponseEntity.ok(new MessageResponse("Logout successful"));
    }

    @PostMapping("/logout-device")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Logout from current device",
            description = "Logs out the user from the current device by invalidating all tokens associated with this device."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Device logout successful",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content
            )
    })
    public ResponseEntity<MessageResponse> logoutFromDevice(HttpServletRequest request) {
        String token = HttpUtil.extractTokenFromRequest(request);
        if (token != null) {
            authService.logoutFromDevice(token);
        }
        return ResponseEntity.ok(new MessageResponse("Device logout successful"));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get current user information",
            description = "Retrieves the profile information of the currently authenticated user."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User information retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content
            )
    })
    public ResponseEntity<UserResponse> getCurrentUser() {
        UserResponse user = authService.getCurrentUser();
        return ResponseEntity.ok(user);
    }

    @PostMapping("/validate-token")
    @Operation(
            summary = "Validate JWT token",
            description = "Validates a JWT token and returns information about its validity and associated user."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Token validation result",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TokenValidationResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters",
                    content = @Content
            )
    })
    public ResponseEntity<TokenValidationResponse> validateToken(
            @Parameter(description = "JWT token to validate", required = true, example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
            @RequestParam String token) {
        TokenValidationResponse response = authService.validateToken(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Change user password",
            description = "Changes the password for the currently authenticated user. Requires current password verification."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Password changed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters or current password incorrect",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content
            )
    })
    public ResponseEntity<MessageResponse> changePassword(
            @Parameter(description = "Password change request", required = true)
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok(new MessageResponse("Password changed successfully"));
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Request password reset",
            description = "Sends a password reset email to the user if the email exists in the system. Always returns success for security reasons."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Password reset email sent (if email exists)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters",
                    content = @Content
            )
    })
    public ResponseEntity<MessageResponse> forgotPassword(
            @Parameter(description = "Email address for password reset", required = true)
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(new MessageResponse("If your email exists, you will receive a reset link"));
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Reset password with token",
            description = "Resets the user's password using a valid reset token received via email."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Password reset successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters or expired/invalid reset token",
                    content = @Content
            )
    })
    public ResponseEntity<MessageResponse> resetPassword(
            @Parameter(description = "Password reset request with token", required = true)
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponse("Password reset successfully"));
    }

    @PostMapping("/verify-email")
    @Operation(
            summary = "Verify email address",
            description = "Verifies a user's email address using the verification token sent during registration."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Email verified successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid or expired verification token",
                    content = @Content
            )
    })
    public ResponseEntity<MessageResponse> verifyEmail(
            @Parameter(description = "Email verification token", required = true)
            @RequestBody TokenRequest tokenRequest) {
        authService.verifyEmail(tokenRequest.token());
        return ResponseEntity.ok(new MessageResponse("Email verified successfully"));
    }

    @PostMapping("/resend-verification")
    @Operation(
            summary = "Resend email verification",
            description = "Resends the email verification link to the specified email address if it exists and is not already verified."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Verification email sent (if email exists and is unverified)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters",
                    content = @Content
            )
    })
    public ResponseEntity<MessageResponse> resendEmailVerification(
            @Parameter(description = "Email address to resend verification to", required = true)
            @RequestBody EmailRequest emailRequest) {
        authService.resendEmailVerification(emailRequest.email());
        return ResponseEntity.ok(new MessageResponse("Verification email sent"));
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
