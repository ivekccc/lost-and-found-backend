package com.example.demo.shared.exception;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDTO> handleBadCredentials(BadCredentialsException ex) {
        return respond(HttpStatus.UNAUTHORIZED, localize("auth.badCredentials"));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponseDTO> handleDisabled(DisabledException ex) {
        return respond(HttpStatus.FORBIDDEN, localize("auth.accountInactive"));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDTO> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return respond(HttpStatus.CONFLICT, localize(ex));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getDefaultMessage())
            .orElseGet(() -> localize("error.validationFailed"));

        return respond(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponseDTO> handleBadRequestParams(Exception ex) {
        return respond(HttpStatus.BAD_REQUEST, localize("error.invalidParameter"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(IllegalArgumentException ex) {
        return respond(HttpStatus.BAD_REQUEST, localize(ex.getMessage()));
    }

    @ExceptionHandler(InvalidVerificationException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidVerification(InvalidVerificationException ex) {
        return respond(HttpStatus.BAD_REQUEST, localize(ex));
    }

    @ExceptionHandler(EmailSendException.class)
    public ResponseEntity<ErrorResponseDTO> handleEmailSendException(EmailSendException ex) {
        return respond(HttpStatus.SERVICE_UNAVAILABLE, localize(ex));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidToken(InvalidTokenException ex) {
        return respond(HttpStatus.UNAUTHORIZED, localize(ex));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleOtherExceptions(Exception ex) {
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, localize("error.unexpected"));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFound(ResourceNotFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, localize(ex));
    }

    @ExceptionHandler(LocationOutsideSupportedCitiesException.class)
    public ResponseEntity<ErrorResponseDTO> handleLocationOutsideSupportedCities(
            LocationOutsideSupportedCitiesException ex) {
        return respond(HttpStatus.CONFLICT, localize(ex));
    }

    @ExceptionHandler(InvalidClaimException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidClaim(InvalidClaimException ex) {
        return respond(HttpStatus.BAD_REQUEST, localize(ex));
    }

    @ExceptionHandler(InvalidChallengeException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidChallenge(InvalidChallengeException ex) {
        return respond(HttpStatus.BAD_REQUEST, localize(ex));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponseDTO> handleRateLimitExceeded(RateLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(new ErrorResponseDTO(localize(ex), HttpStatus.TOO_MANY_REQUESTS.value()));
    }

    @ExceptionHandler(InvalidAbuseReportException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidAbuseReport(InvalidAbuseReportException ex) {
        return respond(HttpStatus.BAD_REQUEST, localize(ex));
    }

    @ExceptionHandler(InvalidSavedSearchException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidSavedSearch(InvalidSavedSearchException ex) {
        return respond(HttpStatus.BAD_REQUEST, localize(ex));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponseDTO> handleDuplicateResource(DuplicateResourceException ex) {
        return respond(HttpStatus.CONFLICT, localize(ex));
    }

    @ExceptionHandler(AccountRestrictedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccountRestricted(AccountRestrictedException ex) {
        return respond(HttpStatus.FORBIDDEN, localize(ex));
    }

    private ResponseEntity<ErrorResponseDTO> respond(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponseDTO(message, status.value()));
    }

    private String localize(LocalizedException ex) {
        return localize(ex.getMessage(), ex.getArguments());
    }

    private String localize(String key, Object... arguments) {
        if (key == null) {
            return localize("error.unexpected");
        }
        try {
            return messageSource.getMessage(key, arguments, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            return key;
        }
    }
}
