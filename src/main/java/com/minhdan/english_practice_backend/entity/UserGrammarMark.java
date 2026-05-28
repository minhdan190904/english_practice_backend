package com.minhdan.english_practice_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "user_grammar_marks",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "lesson_id"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserGrammarMark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(name = "lesson_id", nullable = false)
    private Integer lessonId;

    @Column(name = "marked", nullable = false)
    @Builder.Default
    private Boolean marked = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
