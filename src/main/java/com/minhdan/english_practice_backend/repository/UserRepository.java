package com.minhdan.english_practice_backend.repository;

import com.minhdan.english_practice_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByFirebaseUid(String firebaseUid);
    Optional<User> findByDeviceId(String deviceId);
    Optional<User> findByEmail(String email);
    long countByIsAnonymousTrue();
    long countByIsAnonymousFalse();
    long countByUpdatedAtAfter(LocalDateTime after);
    List<User> findAllByOrderByCreatedAtDesc();
}
