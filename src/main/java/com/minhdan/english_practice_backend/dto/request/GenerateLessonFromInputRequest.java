package com.minhdan.english_practice_backend.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class GenerateLessonFromInputRequest {
    private String inputText;
    private String level;
    private List<String> learnedWords; // optional, words user already knows
}
