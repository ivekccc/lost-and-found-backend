package com.example.demo.service;

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
 * Izdavanje i provera JWT tokena.
 *
 * Access i refresh token nose claim {@code typ} i provere ga zahtevaju. Bez njega su se dva
 * tokena razlikovala samo po tome sto refresh nema {@code role} claim, a provera je gledala
 * iskljucivo subject — pa je refresh token radio kao Authorization header na svakom endpointu,
 * ukljucujuci /admin/**. Kako refresh vazi 7 dana, njegovo curenje je znacilo nedelju dana punog
 * pristupa. Vazilo je i obrnuto: access token se mogao zameniti za nov refresh unedogled.
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
     * Kljuc se pravi jednom, iz SIROVIH bajtova tajne.
     *
     * Stari jjwt (0.9.1) je string tajne base64-dekodirao i prihvatao kljuc bilo koje duzine,
     * pa je HS256 mogao raditi sa materijalom slabijim nego sto standard trazi. Nova verzija
     * to odbija — poruka se ovde hvata i prevodi u konkretnu, jer bi izvorna govorila o
     * bajtovima kljuca, a ne o promenljivoj koju treba popraviti.
     *
     * Posledica prelaska: kljuc se sada tumaci drugacije nego ranije, pa svi tokeni izdati pre
     * ove promene prestaju da vaze i korisnici se jednom preusmere na login.
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

    /**
     * Tokeni izdati pre uvodenja {@code typ} claim-a nemaju tu vrednost i odbijaju se.
     */
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
