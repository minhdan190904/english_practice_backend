package com.minhdan.english_practice_backend.dto.request;

import com.minhdan.english_practice_backend.entity.enums.WordStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SyncVocabularyRequest {
    private String word;
    private WordStatus status;
    private String userDefinition;
    private String wordDataJson;
    private LocalDateTime nextReviewDate;
}
