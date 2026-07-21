package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.event.GoogleAvatarRequestedEvent;
import com.example.demo.exception.InvalidTokenException;
import com.example.demo.exception.InvalidVerificationException;
import com.example.demo.exception.RateLimitExceededException;
import com.example.demo.exception.UserAlreadyExistsException;
import com.example.demo.model.PasswordReset;
import com.example.demo.model.PreRegistration;
import com.example.demo.model.User;
import com.example.demo.model.UserRole;
import com.example.demo.model.UserStatus;
import com.example.demo.repository.PasswordResetRepository;
import com.example.demo.repository.PreRegistrationRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.VerificationCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PreRegistrationRepository preRegistrationRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final EmailService emailService;
    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final ApplicationEventPublisher eventPublisher;

    private static final int RESEND_COOLDOWN_SECONDS = 60;

    @Value("${app.verification.expiry-minutes:15}")
    private int codeExpiryMinutes;

    @Transactional
    public void register(RegisterRequestDTO req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        preRegistrationRepository.deleteByEmail(req.getEmail());

        String code = VerificationCodeGenerator.generateVerificationCode();

        PreRegistration preReg = new PreRegistration();
        preReg.setEmail(req.getEmail());
        preReg.setUsername(req.getUsername());
        preReg.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        preReg.setFirstName(req.getFirstName());
        preReg.setLastName(req.getLastName());
        preReg.setPhoneNumber(req.getPhoneNumber());
        preReg.setVerificationCode(code);
        preReg.setExpiresAt(LocalDateTime.now().plusMinutes(codeExpiryMinutes));

        preRegistrationRepository.save(preReg);

        emailService.sendVerificationEmail(req.getEmail(), code);
    }

    @Transactional
    public void resendCode(ResendCodeRequestDto req) {
        PreRegistration preReg = preRegistrationRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new InvalidVerificationException(
                        "No pending registration for this email"));

        LocalDateTime sentAt = preReg.getExpiresAt().minusMinutes(codeExpiryMinutes);
        LocalDateTime retryAt = sentAt.plusSeconds(RESEND_COOLDOWN_SECONDS);
        LocalDateTime now = LocalDateTime.now();
        if (retryAt.isAfter(now)) {
            throw new RateLimitExceededException(
                    "Please wait a minute before requesting a new code",
                    Duration.between(now, retryAt).toSeconds());
        }

        String code = VerificationCodeGenerator.generateVerificationCode();
        preReg.setVerificationCode(code);
        preReg.setExpiresAt(LocalDateTime.now().plusMinutes(codeExpiryMinutes));
        preRegistrationRepository.save(preReg);

        emailService.sendVerificationEmail(preReg.getEmail(), code);
    }

    @Transactional
    public AuthResponseDTO verifyCode(VerifyRequestDTO req) {

        PreRegistration preReg = preRegistrationRepository
                .findByVerificationCode(req.getCode())
                .orElseThrow(() -> new InvalidVerificationException("Invalid verification code"));

        if (preReg.getExpiresAt().isBefore(LocalDateTime.now())) {
            preRegistrationRepository.delete(preReg);
            throw new InvalidVerificationException("Verification code expired");
        }
        User user = new User();
        user.setEmail(preReg.getEmail());
        user.setUsername(preReg.getUsername());
        user.setPassword(preReg.getPasswordHash());
        user.setFirstName(preReg.getFirstName());
        user.setLastName(preReg.getLastName());
        user.setPhoneNumber(preReg.getPhoneNumber());
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setRole(UserRole.USER);
        userRepository.save(user);

        preRegistrationRepository.delete(preReg);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
        return new AuthResponseDTO(token, refreshToken, "Email verified successfully", user.getRole().name());

    }

    public AuthResponseDTO login(AuthRequestDTO req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        String token = jwtUtil.generateToken(req.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(req.getEmail());
        return new AuthResponseDTO(token, refreshToken, "Login successful", user.getRole().name());
    }

    @Transactional
    public AuthResponseDTO googleLogin(GoogleAuthRequestDto req) {
        GoogleTokenVerifierService.GoogleIdentity identity =
                googleTokenVerifierService.verify(req.getIdToken());

        User user = userRepository.findByGoogleSub(identity.subject())
                .or(() -> userRepository.findByEmail(identity.email()))
                .orElse(null);

        if (user == null) {
            user = createGoogleUser(identity);
        } else {
            if (user.getStatus() == UserStatus.BLOCKED || user.getStatus() == UserStatus.DELETED) {
                throw new DisabledException("User account is disabled");
            }
            if (user.getGoogleSub() == null) {
                user.setGoogleSub(identity.subject());
            }
        }

        if (user.getAvatarUrl() == null && identity.pictureUrl() != null) {
            eventPublisher.publishEvent(
                    new GoogleAvatarRequestedEvent(this, user.getId(), identity.pictureUrl()));
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
        return new AuthResponseDTO(token, refreshToken, "Login successful", user.getRole().name());
    }

    private User createGoogleUser(GoogleTokenVerifierService.GoogleIdentity identity) {
        User user = new User();
        user.setEmail(identity.email());
        user.setUsername(generateUniqueUsername(identity.email()));
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setFirstName(identity.firstName());
        user.setLastName(identity.lastName());
        user.setGoogleSub(identity.subject());
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setRole(UserRole.USER);
        return userRepository.save(user);
    }

    private String generateUniqueUsername(String email) {
        String base = email.substring(0, email.indexOf('@')).replaceAll("[^a-zA-Z0-9._-]", "");
        if (base.isBlank()) {
            base = "user";
        }
        if (base.length() > 40) {
            base = base.substring(0, 40);
        }
        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + suffix++;
        }
        return candidate;
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequestDto req) {
        User user = userRepository.findByEmail(req.getEmail()).orElse(null);

        // Always succeed to the caller: revealing whether an email is registered would let
        // attackers enumerate accounts. We skip sending only for accounts that cannot log in
        // anyway (blocked or deleted) — partially blocked users can still sign in, so they may reset.
        if (user == null
                || user.getStatus() == UserStatus.BLOCKED
                || user.getStatus() == UserStatus.DELETED) {
            return;
        }

        PasswordReset reset = passwordResetRepository.findByEmail(req.getEmail()).orElse(null);
        LocalDateTime now = LocalDateTime.now();

        if (reset != null && reset.getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(now)) {
            throw new RateLimitExceededException(
                    "Please wait a minute before requesting a new code",
                    Duration.between(now, reset.getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS)).toSeconds());
        }

        String code = VerificationCodeGenerator.generateVerificationCode();
        if (reset == null) {
            reset = new PasswordReset();
            reset.setEmail(req.getEmail());
        }
        reset.setCode(code);
        reset.setCreatedAt(now);
        reset.setExpiresAt(now.plusMinutes(codeExpiryMinutes));
        passwordResetRepository.save(reset);

        emailService.sendPasswordResetEmail(req.getEmail(), code);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequestDto req) {
        PasswordReset reset = passwordResetRepository.findByEmail(req.getEmail())
                .filter(r -> r.getCode().equals(req.getCode()))
                .orElseThrow(() -> new InvalidVerificationException("Invalid or expired code"));

        if (reset.getExpiresAt().isBefore(LocalDateTime.now())) {
            passwordResetRepository.delete(reset);
            throw new InvalidVerificationException("Invalid or expired code");
        }

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new InvalidVerificationException("Invalid or expired code"));

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        passwordResetRepository.delete(reset);
    }

    public RefreshTokenResponseDTO refreshToken(RefreshTokenRequestDTO req) {
        String refreshToken = req.getRefreshToken();
        String username = jwtUtil.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!userDetails.isEnabled() || !jwtUtil.validateRefreshToken(refreshToken, userDetails)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String newAccessToken = jwtUtil.generateToken(username, user.getRole().name());
        String newRefreshToken = jwtUtil.generateRefreshToken(username);
        return new RefreshTokenResponseDTO(newAccessToken, newRefreshToken, "Token refreshed");
    }
}
