package com.minhdan.english_practice_backend.repository;

import com.minhdan.english_practice_backend.entity.RefreshToken;
import com.minhdan.english_practice_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    /** Revoke all tokens for a user (used on logout or password change). */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user AND rt.revoked = false")
    void revokeAllByUser(User user);

    /** Clean up expired tokens. */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now OR rt.revoked = true")
    void deleteExpiredOrRevoked(LocalDateTime now);

    void deleteByUser(User user);
}
