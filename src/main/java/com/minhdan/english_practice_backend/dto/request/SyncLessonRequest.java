package com.minhdan.english_practice_backend.dto.request;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SyncLessonRequest {
    private String lessonId;
    private String title;
    private String passage;
    private String passageVi;
    private String imageBase64;
    private String imageUrl;
    private List<Map<String, Object>> words;
    private List<Map<String, String>> sentences;
    private String createdAt;
}
