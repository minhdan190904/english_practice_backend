package com.minhdan.english_practice_backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class LessonDto {
    private String lessonId;
    private String title;
    private String passage;
    private String passageVi;
    private String imageBase64;
    private List<Map<String, Object>> words;
    private String createdAt;
}
