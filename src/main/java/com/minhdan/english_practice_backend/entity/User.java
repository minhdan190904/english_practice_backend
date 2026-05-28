package com.minhdan.english_practice_backend.entity;

import com.minhdan.english_practice_backend.entity.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int ID_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Id
    @Column(length = 8)
    private String id;

    @Column(name = "device_id", unique = true)
    private String deviceId;

    @Column(name = "firebase_uid", unique = true)
    private String firebaseUid;

    @Column(name = "email")
    private String email;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Builder.Default
    @Column(name = "is_anonymous", nullable = false)
    private Boolean isAnonymous = true;

    @Column(name = "password")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void generateId() {
        if (this.id == null) {
            StringBuilder sb = new StringBuilder(ID_LENGTH);
            for (int i = 0; i < ID_LENGTH; i++) {
                sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
            }
            this.id = sb.toString();
        }
    }
}
