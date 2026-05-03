package com.minhdan.english_practice_backend.dto;

import lombok.Data;

@Data
public class GenerateLessonRequest {
    private String topic;
    private String level;
    private String customText;
}
