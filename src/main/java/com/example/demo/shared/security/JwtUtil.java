package com.example.demo.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
/**
 * Issues and validates JWTs. Both token kinds carry a typ claim and every check requires it —
 * without it the two differed only by a role claim, so a refresh token worked as an Authorization
 * header on every endpoint, /admin/** included, for seven days.
 */
@Component
public class JwtUtil {

    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    /** HS256 po specifikaciji trazi kljuc bar koliko i duzina digest-a. */
    private static final int MIN_SECRET_BYTES = 32;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token.expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token.expiration-ms}")
    private long refreshTokenExpirationMs;

    private SecretKey signingKey;

    /**
     * Built once from the RAW secret bytes. jjwt 0.9.1 base64-decoded the string and silently
     * accepted keys shorter than the digest; the length is now checked at startup instead.
     */
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

    public String generateToken(String email, String role) {
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .subject(email)
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpirationMs))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public boolean validateRefreshToken(String token, UserDetails userDetails) {
        return isTokenOfType(token, REFRESH_TOKEN_TYPE)
                && extractUsername(token).equals(userDetails.getUsername());
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        return isTokenOfType(token, ACCESS_TOKEN_TYPE)
                && extractUsername(token).equals(userDetails.getUsername());
    }

    /** Tokens issued before the typ claim existed do not carry it and are rejected. */
    private boolean isTokenOfType(String token, String expectedType) {
        return expectedType.equals(parseClaims(token).get(TOKEN_TYPE_CLAIM, String.class));
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
