package com.minhdan.english_practice_backend.controller;

import com.minhdan.english_practice_backend.dto.ProgressLogRequest;
import com.minhdan.english_practice_backend.dto.ProgressSummaryResponse;
import com.minhdan.english_practice_backend.entity.User;
import com.minhdan.english_practice_backend.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @PostMapping("/log-session")
    public ResponseEntity<Void> logSession(
            @AuthenticationPrincipal User currentUser,
            @RequestBody ProgressLogRequest request) {
        progressService.logSession(currentUser, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/summary")
    public ResponseEntity<ProgressSummaryResponse> getSummary(
            @AuthenticationPrincipal User currentUser) {
        ProgressSummaryResponse summary = progressService.getProgressSummary(currentUser);
        return ResponseEntity.ok(summary);
    }
}
