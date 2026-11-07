package com.example.demo.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
@Component
public class OneTimeCodeHasher {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String KEY_LABEL = "one-time-code:";

    private final SecretKeySpec key;

    public OneTimeCodeHasher(@Value("${jwt.secret}") String secret) {
        try {
            byte[] derived = MessageDigest.getInstance("SHA-256")
                    .digest((KEY_LABEL + secret).getBytes(StandardCharsets.UTF_8));
            this.key = new SecretKeySpec(derived, ALGORITHM);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public String hash(String email, String code) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            byte[] digest = mac.doFinal((email + ":" + normalize(code)).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC unavailable", e);
        }
    }

    public boolean matches(String email, String code, String expectedHash) {
        if (code == null || expectedHash == null) {
            return false;
        }
        return MessageDigest.isEqual(
                hash(email, code).getBytes(StandardCharsets.US_ASCII),
                expectedHash.getBytes(StandardCharsets.US_ASCII));
    }

    private static String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }
}
