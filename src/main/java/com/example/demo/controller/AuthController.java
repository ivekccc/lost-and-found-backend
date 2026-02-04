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
}
