package com.minhdan.english_practice_backend.dto;

import lombok.Data;

@Data
public class ReportRequest {
    private String type;       // "passage", "word", "grammar", "other"
    private String content;    // The reported content (passage title, word, etc.)
    private String reason;     // User's reason for reporting
    private String userId;     // Optional: Firebase UID
}
