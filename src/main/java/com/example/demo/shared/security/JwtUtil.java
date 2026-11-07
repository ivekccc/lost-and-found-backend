package com.example.demo.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
@Component
public class JwtUtil {

    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String TOKEN_VERSION_CLAIM = "ver";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final int MIN_SECRET_BYTES = 32;
    private static final long ALLOWED_CLOCK_SKEW_SECONDS = 30;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.access-token.expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token.expiration-ms}")
    private long refreshTokenExpirationMs;

    private SecretKey signingKey;

    @PostConstruct
    void initSigningKey() {
        byte[] keyBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET ima " + keyBytes.length + " bajtova, a HS256 trazi najmanje "
                            + MIN_SECRET_BYTES + ". Postavi duzu vrednost u okruzenju.");
        }
        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(AuthenticatedUser user) {
        return Jwts.builder()
                .issuer(issuer)
                .subject(user.email())
                .claim("role", user.role())
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .claim(TOKEN_VERSION_CLAIM, user.tokenVersion())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject(email)
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpirationMs))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public boolean validateRefreshToken(String token, AuthenticatedUser user) {
        Claims claims = parseClaims(token);
        return REFRESH_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))
                && user.email().equals(claims.getSubject());
    }

    public boolean validateToken(String token, AuthenticatedUser user) {
        Claims claims = parseClaims(token);
        Integer version = claims.get(TOKEN_VERSION_CLAIM, Integer.class);
        return ACCESS_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))
                && user.email().equals(claims.getSubject())
                && version != null
                && version == user.tokenVersion();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public LocalDateTime extractExpiration(String token) {
        return LocalDateTime.ofInstant(parseClaims(token).getExpiration().toInstant(), ZoneId.systemDefault());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .clockSkewSeconds(ALLOWED_CLOCK_SKEW_SECONDS)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
