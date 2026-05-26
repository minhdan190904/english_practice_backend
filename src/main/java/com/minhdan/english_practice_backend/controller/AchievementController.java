package com.minhdan.english_practice_backend.controller;

import com.minhdan.english_practice_backend.dto.request.SyncAchievementRequest;
import com.minhdan.english_practice_backend.dto.response.AchievementDto;
import com.minhdan.english_practice_backend.entity.User;
import com.minhdan.english_practice_backend.service.AchievementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/achievements")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    /**
     * Sync achievements from client (upsert).
     * Client sends all local achievements, server merges and returns full list.
     */
    @PostMapping("/sync")
    public ResponseEntity<List<AchievementDto>> syncAchievements(
            @AuthenticationPrincipal User currentUser,
            @RequestBody List<SyncAchievementRequest> requests) {
        List<AchievementDto> achievements = achievementService.syncAchievements(currentUser, requests);
        return ResponseEntity.ok(achievements);
    }

    /**
     * Get all achievements for the current user.
     */
    @GetMapping
    public ResponseEntity<List<AchievementDto>> getAchievements(@AuthenticationPrincipal User currentUser) {
        List<AchievementDto> achievements = achievementService.getUserAchievements(currentUser);
        return ResponseEntity.ok(achievements);
    }
}
