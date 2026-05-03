package com.minhdan.english_practice_backend.controller;

import com.minhdan.english_practice_backend.dto.GenerateLessonRequest;
import com.minhdan.english_practice_backend.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/generate-lesson")
    public ResponseEntity<String> generateLesson(@RequestBody GenerateLessonRequest request) {
        String result = aiService.generateLesson(request.getTopic(), request.getLevel(), request.getCustomText());
        return ResponseEntity.ok(result); // Returns the JSON string directly
    }
}
