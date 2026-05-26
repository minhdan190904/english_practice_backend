package com.minhdan.english_practice_backend.dto.response;

import com.minhdan.english_practice_backend.entity.enums.WordStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserVocabularyDto {
    private Long id;
    private String word;
    private WordStatus status;
    private String userDefinition;
    private String wordDataJson;
    private LocalDateTime nextReviewDate;
    private Double easeFactor;
    private Integer srsInterval;
    private Integer repetitions;
    private LocalDateTime lastReviewDate;
}
