package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.exception.InvalidVerificationException;
import com.example.demo.exception.UserAlreadyExistsException;
import com.example.demo.model.PreRegistration;
import com.example.demo.model.User;
import com.example.demo.model.UserStatus;
import com.example.demo.repository.PreRegistrationRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.VerificationCodeGenerator;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PreRegistrationRepository preRegistrationRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final EmailService emailService;

    @Value("${app.verification.expiry-minutes:15}")
    private int codeExpiryMinutes;

    @Transactional
    public MessageResponseDTO register(RegisterRequestDTO req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        preRegistrationRepository.findByEmail(req.getEmail())
                .ifPresent(preRegistrationRepository::delete);

        String code = VerificationCodeGenerator.generateVerificationCode(6);

        PreRegistration preReg = new PreRegistration();
        preReg.setEmail(req.getEmail());
        preReg.setUsername(req.getUsername());
        preReg.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        preReg.setVerificationCode(code);
        preReg.setExpiresAt(LocalDateTime.now().plusMinutes(codeExpiryMinutes));

        preRegistrationRepository.save(preReg);

        emailService.sendVerificationEmail(req.getEmail(), code);

        return new MessageResponseDTO("Verification code sent to email");
    }

    @Transactional
    public AuthResponseDTO verifyCode(VerifyRequestDTO req) {

        PreRegistration preReg = preRegistrationRepository
                .findByEmailAndVerificationCode(req.getEmail(), req.getCode())
                .orElseThrow(() -> new InvalidVerificationException("Invalid verification code"));

        if (preReg.getExpiresAt().isBefore(LocalDateTime.now())) {
            preRegistrationRepository.delete(preReg);
            throw new InvalidVerificationException("Verification code expired");
        }
        User user = new User();
        user.setEmail(preReg.getEmail());
        user.setUsername(preReg.getUsername());
        user.setPassword(preReg.getPasswordHash());
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        preRegistrationRepository.delete(preReg);

        String token = jwtUtil.generateToken(user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
        return new AuthResponseDTO(token, refreshToken, "Email verified successfully");

    }

    public AuthResponseDTO login(AuthRequestDTO req) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );
        String token = jwtUtil.generateToken(req.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(req.getEmail());
        return new AuthResponseDTO(token, refreshToken, "Login successful");
    }

    public RefreshTokenResponseDTO refreshToken(RefreshTokenRequestDTO req) {
        String refreshToken = req.getRefreshToken();
        String username = jwtUtil.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!jwtUtil.validateRefreshToken(refreshToken, userDetails)) {
            return new RefreshTokenResponseDTO(null, null, "Invalid refresh token");
        }
        String newAccessToken = jwtUtil.generateToken(username);
        String newRefreshToken = jwtUtil.generateRefreshToken(username);
        return new RefreshTokenResponseDTO(newAccessToken, newRefreshToken, "Token refreshed");
    }
}
