package com.example.demo.service;

import com.example.demo.exception.InvalidTokenException;
import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.api.client.json.webtoken.JsonWebToken;
import com.google.auth.oauth2.TokenVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class GoogleTokenVerifierService {

    private static final Set<String> ALLOWED_ISSUERS =
            Set.of("https://accounts.google.com", "accounts.google.com");

    private final TokenVerifier tokenVerifier;

    public GoogleTokenVerifierService(@Value("${app.google.client-id}") String clientId) {
        this.tokenVerifier = TokenVerifier.newBuilder()
                .setAudience(clientId)
                .build();
    }

    public GoogleIdentity verify(String idToken) {
        JsonWebSignature signature;
        try {
            signature = tokenVerifier.verify(idToken);
        } catch (TokenVerifier.VerificationException e) {
            throw new InvalidTokenException("Invalid Google token");
        }

        JsonWebToken.Payload payload = signature.getPayload();

        if (!ALLOWED_ISSUERS.contains(payload.getIssuer())) {
            throw new InvalidTokenException("Invalid Google token issuer");
        }
        if (!Boolean.parseBoolean(String.valueOf(payload.get("email_verified")))) {
            throw new InvalidTokenException("Google account email is not verified");
        }

        String email = (String) payload.get("email");
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new InvalidTokenException("Google token does not contain an email");
        }

        return new GoogleIdentity(
                payload.getSubject(),
                email,
                (String) payload.get("given_name"),
                (String) payload.get("family_name"),
                (String) payload.get("picture"));
    }

    public record GoogleIdentity(
            String subject,
            String email,
            String firstName,
            String lastName,
            String pictureUrl) {
    }
}
