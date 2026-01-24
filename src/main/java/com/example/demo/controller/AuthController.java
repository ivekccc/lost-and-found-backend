package com.example.demo.controller;

import com.example.demo.dto.AuthRequestDTO;
import com.example.demo.dto.RegisterRequestDTO;
import com.example.demo.dto.AuthResponseDTO;
import com.example.demo.dto.RefreshTokenRequestDTO;
import com.example.demo.dto.RefreshTokenResponseDTO;
import com.example.demo.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO req) {
        AuthResponseDTO response = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponseDTO> refresh(@RequestBody RefreshTokenRequestDTO req) {
        RefreshTokenResponseDTO response = authService.refreshToken(req);
        if (response.getAccessToken() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO req) {
        AuthResponseDTO response = authService.login(req);
        return ResponseEntity.ok(response);
    }
}
