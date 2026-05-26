package com.minhdan.english_practice_backend.controller;

import com.minhdan.english_practice_backend.entity.User;
import com.minhdan.english_practice_backend.entity.UserStreak;
import com.minhdan.english_practice_backend.service.StreakService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/streak")
@RequiredArgsConstructor
public class StreakController {

    private final StreakService streakService;

    /**
     * Get current streak data.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getStreak(@AuthenticationPrincipal User currentUser) {
        UserStreak streak = streakService.getStreak(currentUser);
        return ResponseEntity.ok(toMap(streak));
    }

    /**
     * Check-in for today (increments streak if consecutive).
     */
    @PostMapping("/check-in")
    public ResponseEntity<Map<String, Object>> checkIn(@AuthenticationPrincipal User currentUser) {
        UserStreak streak = streakService.checkIn(currentUser);
        return ResponseEntity.ok(toMap(streak));
    }

    private Map<String, Object> toMap(UserStreak streak) {
        return Map.of(
                "currentStreak", streak.getCurrentStreak() != null ? streak.getCurrentStreak() : 0,
                "maxStreak", streak.getMaxStreak() != null ? streak.getMaxStreak() : 0,
                "lastActivityDate", streak.getLastActivityDate() != null ? streak.getLastActivityDate().toString() : ""
        );
    }
}
