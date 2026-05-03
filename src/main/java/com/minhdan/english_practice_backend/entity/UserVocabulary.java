package com.minhdan.english_practice_backend.entity;

import com.minhdan.english_practice_backend.entity.enums.WordStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "user_vocabularies",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "word"})
    },
    indexes = {
        @Index(name = "idx_word", columnList = "word")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVocabulary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(name = "word", nullable = false)
    private String word;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WordStatus status;

    @Column(name = "user_definition", columnDefinition = "TEXT")
    private String userDefinition;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "word_data_json", columnDefinition = "json")
    private String wordDataJson; // Using String to hold the raw JSON. Alternatively we could use a mapped POJO or JsonNode, but String is easiest for generic passing.

    @Column(name = "next_review_date")
    private LocalDateTime nextReviewDate;
}
