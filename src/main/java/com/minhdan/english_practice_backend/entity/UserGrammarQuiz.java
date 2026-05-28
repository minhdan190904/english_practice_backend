package com.minhdan.english_practice_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "user_grammar_quizzes",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "topic_id"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserGrammarQuiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(name = "topic_id", nullable = false)
    private Integer topicId;

    @Column(name = "completed", nullable = false)
    @Builder.Default
    private Boolean completed = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
