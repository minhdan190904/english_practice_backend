package com.minhdan.english_practice_backend.security;

import com.minhdan.english_practice_backend.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * JWT service — generates and verifies HS256 tokens.
 * Access token: short-lived (15 min default).
 * Refresh token: long-lived (30 days default).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtKeyProvider keyProvider;

    @Value("${jwt.access-token-expiration-ms:3600000}")  // 1 hour
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration-ms:2592000000}")  // 30 days
    private long refreshTokenExpirationMs;

    /**
     * Generate access token for a user.
     */
    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("uid", user.getFirebaseUid())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("type", "ACCESS")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(keyProvider.getSecretKey())
                .compact();
    }

    /**
     * Generate refresh token for a user.
     */
    public String generateRefreshToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpirationMs);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("type", "REFRESH")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(keyProvider.getSecretKey())
                .compact();
    }

    /**
     * Parse and validate a token. Returns claims if valid.
     * @throws JwtException if token is invalid or expired
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(keyProvider.getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validate token without throwing. Returns true if valid.
     */
    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("JWT signature invalid: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("JWT invalid: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Extract user ID from token (even if expired, for refresh flow).
     */
    public String extractUserId(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * Extract token type ("ACCESS" or "REFRESH").
     */
    public String extractTokenType(String token) {
        Claims claims = parseToken(token);
        return claims.get("type", String.class);
    }

    /**
     * Get refresh token expiration in milliseconds.
     */
    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }
}
