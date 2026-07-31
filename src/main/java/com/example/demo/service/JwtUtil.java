package com.example.demo.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Izdavanje i provera JWT tokena.
 *
 * Access i refresh token nose claim {@code typ} i provere ga zahtevaju. Bez njega su dva
 * tokena bila razlicita samo po tome sto refresh nema {@code role} claim, a provera je
 * gledala iskljucivo subject — pa je refresh token radio kao Authorization header na svakom
 * endpointu, ukljucujuci /admin/**. Kako refresh vazi 7 dana naspram 10 sati i cuva se
 * trajno na uredaju, njegovo curenje je znacilo nedelju dana punog pristupa. Vazilo je i
 * obrnuto: access token se mogao zameniti za nov refresh i tako produzavati unedogled.
 */
@Component
public class JwtUtil {

    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token.expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token.expiration-ms}")
    private long refreshTokenExpirationMs;

    public String generateToken(String email, String role) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpirationMs))
                .signWith(SignatureAlgorithm.HS256, secret)
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
     * Tokeni izdati pre uvodenja {@code typ} claim-a nemaju tu vrednost. Takav token se
     * odbija, pa se korisnici sa starom sesijom jednom preusmere na login.
     */
    private boolean isTokenOfType(String token, String expectedType) {
        return expectedType.equals(parseClaims(token).get(TOKEN_TYPE_CLAIM, String.class));
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    }
}
