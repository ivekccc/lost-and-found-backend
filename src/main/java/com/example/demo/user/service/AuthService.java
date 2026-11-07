package com.example.demo.user.service;

import com.example.demo.location.LocationFacade;
import com.example.demo.location.model.City;
import com.example.demo.notification.NotificationFacade;
import com.example.demo.shared.concurrency.AdvisoryLocks;
import com.example.demo.shared.concurrency.LockScope;
import com.example.demo.shared.exception.InvalidTokenException;
import com.example.demo.shared.exception.InvalidVerificationException;
import com.example.demo.shared.exception.RateLimitExceededException;
import com.example.demo.shared.exception.UserAlreadyExistsException;
import com.example.demo.shared.security.JwtUtil;
import com.example.demo.shared.util.VerificationCodeGenerator;
import com.example.demo.user.dto.AuthRequestDTO;
import com.example.demo.user.dto.AuthResponseDTO;
import com.example.demo.user.dto.ForgotPasswordRequestDto;
import com.example.demo.user.dto.GoogleAuthRequestDto;
import com.example.demo.user.dto.LogoutRequestDto;
import com.example.demo.user.dto.RefreshTokenRequestDTO;
import com.example.demo.user.dto.RefreshTokenResponseDTO;
import com.example.demo.user.dto.RegisterRequestDTO;
import com.example.demo.user.dto.ResendCodeRequestDto;
import com.example.demo.user.dto.ResetPasswordRequestDto;
import com.example.demo.user.dto.VerifyRequestDTO;
import com.example.demo.user.event.GoogleAvatarRequestedEvent;
import com.example.demo.user.model.AppLanguage;
import com.example.demo.user.model.PasswordReset;
import com.example.demo.user.model.PreRegistration;
import com.example.demo.user.model.User;
import com.example.demo.user.model.UserRole;
import com.example.demo.user.model.UserStatus;
import com.example.demo.user.repository.PasswordResetRepository;
import com.example.demo.user.repository.PreRegistrationRepository;
import com.example.demo.user.repository.UserRepository;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
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
    private final NotificationFacade notificationFacade;
    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final ApplicationEventPublisher eventPublisher;
    private final LocationFacade locationFacade;
    private final RefreshTokenService refreshTokenService;
    private final AdvisoryLocks advisoryLocks;
    private final LoginAttemptService loginAttemptService;

    private static final int RESEND_COOLDOWN_SECONDS = 60;

    @Value("${app.verification.expiry-minutes:15}")
    private int codeExpiryMinutes;

    @Value("${app.cities.default-code:BG}")
    private String defaultCityCode;

    @Transactional
    public void register(RegisterRequestDTO req) {
        advisoryLocks.lock(LockScope.REGISTRATION, req.getEmail());
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("auth.emailExists");
        }

        
        preRegistrationRepository.findByEmail(req.getEmail()).ifPresent(existing -> {
            LocalDateTime sentAt = existing.getExpiresAt().minusMinutes(codeExpiryMinutes);
            LocalDateTime retryAt = sentAt.plusSeconds(RESEND_COOLDOWN_SECONDS);
            LocalDateTime now = LocalDateTime.now();
            if (retryAt.isAfter(now)) {
                throw new RateLimitExceededException(
                        "auth.codeJustSent",
                        Duration.between(now, retryAt).toSeconds());
            }
        });

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

        notificationFacade.sendVerificationEmail(req.getEmail(), code);
    }

    @Transactional
    public void resendCode(ResendCodeRequestDto req) {
        advisoryLocks.lock(LockScope.REGISTRATION, req.getEmail());
        PreRegistration preReg = preRegistrationRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new InvalidVerificationException(
                        "auth.noPendingRegistration"));

        LocalDateTime sentAt = preReg.getExpiresAt().minusMinutes(codeExpiryMinutes);
        LocalDateTime retryAt = sentAt.plusSeconds(RESEND_COOLDOWN_SECONDS);
        LocalDateTime now = LocalDateTime.now();
        if (retryAt.isAfter(now)) {
            throw new RateLimitExceededException(
                    "auth.waitBeforeNewCode",
                    Duration.between(now, retryAt).toSeconds());
        }

        String code = VerificationCodeGenerator.generateVerificationCode();
        preReg.setVerificationCode(code);
        preReg.setExpiresAt(LocalDateTime.now().plusMinutes(codeExpiryMinutes));
        preRegistrationRepository.save(preReg);

        notificationFacade.sendVerificationEmail(preReg.getEmail(), code);
    }

    @Transactional
    public AuthResponseDTO verifyCode(VerifyRequestDTO req) {

       
        PreRegistration preReg = preRegistrationRepository
                .findByEmailAndVerificationCode(req.getEmail(), req.getCode())
                .orElseThrow(() -> new InvalidVerificationException("auth.invalidVerificationCode"));

        if (preReg.getExpiresAt().isBefore(LocalDateTime.now())) {
            preRegistrationRepository.delete(preReg);
            throw new InvalidVerificationException("auth.verificationCodeExpired");
        }
        User user = new User();
        user.setEmail(preReg.getEmail());
        user.setUsername(preReg.getUsername());
        user.setPassword(preReg.getPasswordHash());
        user.setFirstName(preReg.getFirstName());
        user.setLastName(preReg.getLastName());
        user.setPhoneNumber(preReg.getPhoneNumber());
        user.setStatus(UserStatus.ACTIVE);
        user.setLanguage(AppLanguage.fromLocale(LocaleContextHolder.getLocale()));
        user.setCreatedAt(LocalDateTime.now());
        user.setRole(UserRole.USER);
        user.setActiveCity(defaultCity());
        userRepository.save(user);

        preRegistrationRepository.delete(preReg);

        return issueSession(user);
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public AuthResponseDTO login(AuthRequestDTO req) {
        loginAttemptService.ensureAllowed(req.getEmail());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
            );
        } catch (BadCredentialsException e) {
            loginAttemptService.recordFailure(req.getEmail());
            throw e;
        }
        loginAttemptService.clear(req.getEmail());
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return issueSession(user);
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

        return issueSession(user);
    }

    private AuthResponseDTO issueSession(User user) {
        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
        refreshTokenService.register(user, refreshToken, jwtUtil.extractExpiration(refreshToken));
        return new AuthResponseDTO(accessToken, refreshToken, user.getRole());
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
        user.setLanguage(AppLanguage.fromLocale(LocaleContextHolder.getLocale()));
        user.setCreatedAt(LocalDateTime.now());
        user.setRole(UserRole.USER);
        user.setActiveCity(defaultCity());
        return userRepository.save(user);
    }

    /**
     * The city a new account starts on. The column is NOT NULL so a default must exist; Belgrade is
     * chosen because it is the only city with listings so far, and a wrong guess is cheap — the app
     * offers a switch from GPS straight away.
     */
    private City defaultCity() {
        return locationFacade.requireByCode(defaultCityCode);
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

        advisoryLocks.lock(LockScope.PASSWORD_RESET, req.getEmail());
        PasswordReset reset = passwordResetRepository.findByEmail(req.getEmail()).orElse(null);
        LocalDateTime now = LocalDateTime.now();

        if (reset != null && reset.getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(now)) {
            throw new RateLimitExceededException(
                    "auth.waitBeforeNewCode",
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

        notificationFacade.sendPasswordResetEmail(req.getEmail(), code);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequestDto req) {
        PasswordReset reset = passwordResetRepository.findByEmail(req.getEmail())
                .filter(r -> r.getCode().equals(req.getCode()))
                .orElseThrow(() -> new InvalidVerificationException("auth.invalidOrExpiredCode"));

        if (reset.getExpiresAt().isBefore(LocalDateTime.now())) {
            passwordResetRepository.delete(reset);
            throw new InvalidVerificationException("auth.invalidOrExpiredCode");
        }

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new InvalidVerificationException("auth.invalidOrExpiredCode"));

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        passwordResetRepository.delete(reset);
        refreshTokenService.revokeAllForUser(user.getId());
        loginAttemptService.clear(req.getEmail());
    }

    @Transactional
    public RefreshTokenResponseDTO refreshToken(RefreshTokenRequestDTO req) {
        String refreshToken = req.getRefreshToken();
        String username = usernameFromRefreshToken(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!userDetails.isEnabled() || !jwtUtil.validateRefreshToken(refreshToken, userDetails)) {
            throw new InvalidTokenException("auth.invalidRefreshToken");
        }

        if (!refreshTokenService.consume(refreshToken)) {
            throw new InvalidTokenException("auth.refreshTokenRevoked");
        }

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String newAccessToken = jwtUtil.generateToken(username, user.getRole().name());
        String newRefreshToken = jwtUtil.generateRefreshToken(username);
        refreshTokenService.register(user, newRefreshToken, jwtUtil.extractExpiration(newRefreshToken));
        return new RefreshTokenResponseDTO(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(LogoutRequestDto req) {
        boolean wasLive = refreshTokenService.consume(req.getRefreshToken());

        String fcmToken = req.getFcmToken();
        if (!wasLive || fcmToken == null || fcmToken.isBlank()) {
            return;
        }

        userRepository.findByEmail(usernameFromRefreshToken(req.getRefreshToken()))
                .filter(user -> fcmToken.equals(user.getFcmToken()))
                .ifPresent(user -> {
                    user.setFcmToken(null);
                    userRepository.save(user);
                });
    }

    private String usernameFromRefreshToken(String refreshToken) {
        try {
            return jwtUtil.extractUsername(refreshToken);
        } catch (JwtException e) {
            throw new InvalidTokenException("auth.invalidRefreshToken");
        }
    }
}
