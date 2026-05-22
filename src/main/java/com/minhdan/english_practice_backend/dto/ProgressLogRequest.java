package com.minhdan.english_practice_backend.dto;

import lombok.Data;

@Data
public class ProgressLogRequest {
    private Integer timeSpentSeconds = 0;
    private Integer wordsLearned = 0;
    private Integer lessonsCompleted = 0;
}
