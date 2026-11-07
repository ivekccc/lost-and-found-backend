package com.example.demo.shared.ratelimit;

import com.example.demo.shared.exception.RateLimitExceededException;
import com.example.demo.shared.web.ErrorResponseWriter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.LocaleResolver;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String AUTH_PREFIX = "/auth/";
    private static final Set<String> UNLIMITED_PATHS = Set.of("/auth/refresh", "/auth/logout");
    private static final Map<String, RateLimitPolicy> SPECIFIC_POLICIES = Map.of(
            "/auth/verify", RateLimitPolicy.AUTH_CODE_PER_ADDRESS,
            "/auth/reset-password", RateLimitPolicy.AUTH_CODE_PER_ADDRESS,
            "/auth/register", RateLimitPolicy.AUTH_EMAIL_PER_ADDRESS,
            "/auth/resend", RateLimitPolicy.AUTH_EMAIL_PER_ADDRESS,
            "/auth/forgot-password", RateLimitPolicy.AUTH_EMAIL_PER_ADDRESS);

    private final RateLimiter rateLimiter;
    private final MessageSource messageSource;
    private final LocaleResolver localeResolver;

    public AuthRateLimitFilter(RateLimiter rateLimiter, MessageSource messageSource, LocaleResolver localeResolver) {
        this.rateLimiter = rateLimiter;
        this.messageSource = messageSource;
        this.localeResolver = localeResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !path.startsWith(AUTH_PREFIX) || UNLIMITED_PATHS.contains(path) || "OPTIONS".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String address = request.getRemoteAddr();
        try {
            rateLimiter.consume(RateLimitPolicy.AUTH_PER_ADDRESS, address);
            RateLimitPolicy specific = SPECIFIC_POLICIES.get(request.getServletPath());
            if (specific != null) {
                rateLimiter.consume(specific, address);
            }
        } catch (RateLimitExceededException exception) {
            reject(request, response, exception);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, RateLimitExceededException exception) throws IOException {
        String message = messageSource.getMessage(exception.getMessage(), null, localeResolver.resolveLocale(request));
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(exception.getRetryAfterSeconds()));
        ErrorResponseWriter.write(response, HttpStatus.TOO_MANY_REQUESTS, message);
    }
}
