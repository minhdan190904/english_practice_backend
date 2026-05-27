package com.minhdan.english_practice_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "user_lessons",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "lesson_id"})
    },
    indexes = {
        @Index(name = "idx_user_lessons_user", columnList = "user_id"),
        @Index(name = "idx_user_lessons_created", columnList = "created_at")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(name = "lesson_id", nullable = false, length = 100)
    private String lessonId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "passage", columnDefinition = "TEXT", nullable = false)
    private String passage;

    @Column(name = "passage_vi", columnDefinition = "TEXT")
    private String passageVi;

    @Column(name = "image_base64", columnDefinition = "LONGTEXT")
    private String imageBase64;

    @Column(name = "image_url")
    private String imageUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "words_json", columnDefinition = "json")
    private String wordsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sentences_json", columnDefinition = "json")
    private String sentencesJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
