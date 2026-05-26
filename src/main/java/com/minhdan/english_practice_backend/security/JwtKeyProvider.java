package com.minhdan.english_practice_backend.security;

import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;

/**
 * Provides HMAC-SHA256 secret key for JWT signing.
 * Simple symmetric key approach — one shared secret.
 */
@Slf4j
@Component
public class JwtKeyProvider {

    @Value("${jwt.secret-key}")
    private String secretKeyBase64;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKeyBase64);
        secretKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("🔑 HMAC-SHA256 secret key loaded successfully");
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }
}
