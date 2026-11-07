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
import com.example.demo.shared.security.AuthenticatedUser;
import com.example.demo.shared.security.JwtUtil;
import com.example.demo.shared.security.OneTimeCodeHasher;
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
import com.example.demo.user.event.PasswordResetRequestedEvent;
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
    private final NotificationFacade notificationFacade;
    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final ApplicationEventPublisher eventPublisher;
    private final LocationFacade locationFacade;
    private final RefreshTokenService refreshTokenService;
    private final AdvisoryLocks advisoryLocks;
    private final LoginAttemptService loginAttemptService;
    private final OneTimeCodeHasher oneTimeCodeHasher;

    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final int MAX_CODE_ATTEMPTS = 5;
    private static final Duration MAX_PENDING_REGISTRATION = Duration.ofHours(1);

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

        PreRegistration pending = preRegistrationRepository.findByEmail(req.getEmail())
                .filter(this::isPendingUsable)
                .orElse(null);
        if (pending != null) {
            if (!passwordEncoder.matches(req.getPassword(), pending.getPasswordHash())) {
                throw new UserAlreadyExistsException("auth.registrationPending");
            }
            requireCooldownPassed(pending.getExpiresAt(), "auth.codeJustSent");
            sendNewRegistrationCode(pending);
            return;
        }

        preRegistrationRepository.deleteByEmail(req.getEmail());

        PreRegistration preReg = new PreRegistration();
        preReg.setEmail(req.getEmail());
        preReg.setUsername(req.getUsername());
        preReg.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        preReg.setFirstName(req.getFirstName());
        preReg.setLastName(req.getLastName());
        preReg.setPhoneNumber(req.getPhoneNumber());
        sendNewRegistrationCode(preReg);
    }

    @Transactional
    public void resendCode(ResendCodeRequestDto req) {
        advisoryLocks.lock(LockScope.REGISTRATION, req.getEmail());
        PreRegistration preReg = preRegistrationRepository.findByEmail(req.getEmail())
                .filter(this::isPendingUsable)
                .orElseThrow(() -> new InvalidVerificationException("auth.noPendingRegistration"));
        requireCooldownPassed(preReg.getExpiresAt(), "auth.waitBeforeNewCode");
        sendNewRegistrationCode(preReg);
    }

    private boolean isPendingUsable(PreRegistration preRegistration) {
        LocalDateTime now = LocalDateTime.now();
        return preRegistration.getExpiresAt().isAfter(now)
                && preRegistration.getCreatedAt().plus(MAX_PENDING_REGISTRATION).isAfter(now);
    }

    private void requireCooldownPassed(LocalDateTime expiresAt, String messageKey) {
        LocalDateTime retryAt = expiresAt.minusMinutes(codeExpiryMinutes).plusSeconds(RESEND_COOLDOWN_SECONDS);
        LocalDateTime now = LocalDateTime.now();
        if (retryAt.isAfter(now)) {
            throw new RateLimitExceededException(messageKey, Duration.between(now, retryAt).toSeconds());
        }
    }

    private void sendNewRegistrationCode(PreRegistration preReg) {
        String code = VerificationCodeGenerator.generateVerificationCode();
        preReg.setCodeHash(oneTimeCodeHasher.hash(preReg.getEmail(), code));
        preReg.setFailedAttempts(0);
        preReg.setExpiresAt(LocalDateTime.now().plusMinutes(codeExpiryMinutes));
        preRegistrationRepository.save(preReg);
        notificationFacade.sendVerificationEmail(preReg.getEmail(), code);
    }

    @Transactional(noRollbackFor = InvalidVerificationException.class)
    public AuthResponseDTO verifyCode(VerifyRequestDTO req) {
        advisoryLocks.lock(LockScope.REGISTRATION, req.getEmail());
        PreRegistration preReg = preRegistrationRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new InvalidVerificationException("auth.invalidVerificationCode"));

        if (!isPendingUsable(preReg)) {
            preRegistrationRepository.delete(preReg);
            throw new InvalidVerificationException("auth.verificationCodeExpired");
        }
        if (!oneTimeCodeHasher.matches(preReg.getEmail(), req.getCode(), preReg.getCodeHash())
                || !passwordEncoder.matches(req.getPassword(), preReg.getPasswordHash())) {
            registerFailedAttempt(preReg.getFailedAttempts() + 1,
                    () -> preRegistrationRepository.delete(preReg),
                    () -> preReg.setFailedAttempts(preReg.getFailedAttempts() + 1));
            throw new InvalidVerificationException("auth.invalidVerificationCode");
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

    private void registerFailedAttempt(int attempts, Runnable invalidate, Runnable record) {
        if (attempts >= MAX_CODE_ATTEMPTS) {
            invalidate.run();
            throw new InvalidVerificationException("auth.tooManyCodeAttempts");
        }
        record.run();
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
                .orElseThrow(() -> new BadCredentialsException("auth.badCredentials"));
        return issueSession(user);
    }

    @Transactional
    public AuthResponseDTO googleLogin(GoogleAuthRequestDto req) {
        GoogleTokenVerifierService.GoogleIdentity identity =
                googleTokenVerifierService.verify(req.getIdToken());

        User user = userRepository.findByGoogleSub(identity.subject())
                .orElseGet(() -> linkOrCreateGoogleUser(identity));

        if (user.getStatus() == UserStatus.BLOCKED || user.getStatus() == UserStatus.DELETED) {
            throw new DisabledException("User account is disabled");
        }

        if (user.getAvatarUrl() == null && identity.pictureUrl() != null) {
            eventPublisher.publishEvent(
                    new GoogleAvatarRequestedEvent(this, user.getId(), identity.pictureUrl()));
        }

        return issueSession(user);
    }

    private User linkOrCreateGoogleUser(GoogleTokenVerifierService.GoogleIdentity identity) {
        User existing = userRepository.findByEmail(identity.email()).orElse(null);
        if (existing == null) {
            return createGoogleUser(identity);
        }
        if (existing.getGoogleSub() != null) {
            throw new UserAlreadyExistsException("auth.google.accountMismatch");
        }
        if (!identity.isEmailAuthoritative()) {
            throw new UserAlreadyExistsException("auth.google.linkRequiresPassword");
        }
        existing.setGoogleSub(identity.subject());
        return existing;
    }

    private AuthResponseDTO issueSession(User user) {
        String accessToken = jwtUtil.generateToken(MyUserDetailsService.toAuthenticatedUser(user));
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
        refreshTokenService.startSession(user, refreshToken, jwtUtil.extractExpiration(refreshToken));
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
        advisoryLocks.lock(LockScope.PASSWORD_RESET, req.getEmail());
        User user = userRepository.findByEmail(req.getEmail()).orElse(null);
        if (user == null
                || user.getStatus() == UserStatus.BLOCKED
                || user.getStatus() == UserStatus.DELETED) {
            return;
        }

        PasswordReset reset = passwordResetRepository.findByEmail(req.getEmail()).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        if (reset != null && reset.getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(now)) {
            return;
        }

        String code = VerificationCodeGenerator.generateVerificationCode();
        if (reset == null) {
            reset = new PasswordReset();
            reset.setEmail(req.getEmail());
        }
        reset.setCodeHash(oneTimeCodeHasher.hash(req.getEmail(), code));
        reset.setFailedAttempts(0);
        reset.setCreatedAt(now);
        reset.setExpiresAt(now.plusMinutes(codeExpiryMinutes));
        passwordResetRepository.save(reset);

        eventPublisher.publishEvent(new PasswordResetRequestedEvent(req.getEmail(), code, user.getLanguage().toLocale()));
    }

    @Transactional(noRollbackFor = InvalidVerificationException.class)
    public void resetPassword(ResetPasswordRequestDto req) {
        advisoryLocks.lock(LockScope.PASSWORD_RESET, req.getEmail());
        PasswordReset reset = passwordResetRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new InvalidVerificationException("auth.invalidOrExpiredCode"));

        if (reset.getExpiresAt().isBefore(LocalDateTime.now())) {
            passwordResetRepository.delete(reset);
            throw new InvalidVerificationException("auth.invalidOrExpiredCode");
        }
        if (!oneTimeCodeHasher.matches(reset.getEmail(), req.getCode(), reset.getCodeHash())) {
            registerFailedAttempt(reset.getFailedAttempts() + 1,
                    () -> passwordResetRepository.delete(reset),
                    () -> reset.setFailedAttempts(reset.getFailedAttempts() + 1));
            throw new InvalidVerificationException("auth.invalidOrExpiredCode");
        }

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new InvalidVerificationException("auth.invalidOrExpiredCode"));

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        passwordResetRepository.delete(reset);
        loginAttemptService.clear(req.getEmail());
        refreshTokenService.endAllSessions(user.getId(), true);
    }

    @Transactional(noRollbackFor = InvalidTokenException.class)
    public RefreshTokenResponseDTO refreshToken(RefreshTokenRequestDTO req) {
        String refreshToken = req.getRefreshToken();
        String username = usernameFromRefreshToken(refreshToken);

        RefreshTokenService.RotatedSession session = refreshTokenService.rotate(refreshToken)
                .orElseThrow(() -> new InvalidTokenException("auth.refreshTokenRevoked"));
        User user = session.user();
        AuthenticatedUser principal = MyUserDetailsService.toAuthenticatedUser(user);
        if (!principal.isEnabled() || !jwtUtil.validateRefreshToken(refreshToken, principal)
                || !user.getEmail().equals(username)) {
            throw new InvalidTokenException("auth.invalidRefreshToken");
        }

        String newAccessToken = jwtUtil.generateToken(principal);
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getEmail());
        refreshTokenService.continueSession(session, newRefreshToken, jwtUtil.extractExpiration(newRefreshToken));
        return new RefreshTokenResponseDTO(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(LogoutRequestDto req) {
        String fcmToken = req.getFcmToken();
        refreshTokenService.endSession(req.getRefreshToken())
                .filter(user -> fcmToken != null && fcmToken.equals(user.getFcmToken()))
                .ifPresent(user -> user.setFcmToken(null));
    }

    private String usernameFromRefreshToken(String refreshToken) {
        try {
            return jwtUtil.extractUsername(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("auth.invalidRefreshToken");
        }
    }
}
