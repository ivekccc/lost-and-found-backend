package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication endpoints")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/verify")
    @Operation(summary = "Verify email", description = "Verifies email with code and returns auth tokens")
    public ResponseEntity<AuthResponseDTO> verify(@Valid @RequestBody VerifyRequestDTO req) {
        AuthResponseDTO response = authService.verifyCode(req);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Sends verification code to email")
    @ApiResponse(responseCode = "204", description = "Verification code sent successfully")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequestDTO req) {
        authService.register(req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend")
    @Operation(summary = "Resend verification code",
            description = "Generates a new code for a pending registration and emails it again; throttled to one request per minute")
    @ApiResponse(responseCode = "204", description = "Verification code sent successfully")
    public ResponseEntity<Void> resend(@Valid @RequestBody ResendCodeRequestDto req) {
        authService.resendCode(req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset code",
            description = "Emails a reset code if the account exists; always returns 204 to avoid revealing whether an email is registered. Throttled to one request per minute.")
    @ApiResponse(responseCode = "204", description = "Request accepted")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto req) {
        authService.forgotPassword(req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with code",
            description = "Sets a new password when the emailed code is valid and unexpired")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Password updated"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired code")
    })
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequestDto req) {
        authService.resetPassword(req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Returns new access and refresh tokens")
    public ResponseEntity<RefreshTokenResponseDTO> refresh(@RequestBody RefreshTokenRequestDTO req) {
        return ResponseEntity.ok(authService.refreshToken(req));
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticates user and returns auth tokens")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO req) {
        AuthResponseDTO response = authService.login(req);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    @Operation(summary = "Login with Google",
            description = "Verifies a Google ID token, creates the account on first login and returns auth tokens")
    public ResponseEntity<AuthResponseDTO> google(@Valid @RequestBody GoogleAuthRequestDto req) {
        AuthResponseDTO response = authService.googleLogin(req);
        return ResponseEntity.ok(response);
    }
}
