package com.minhdan.english_practice_backend.controller;

import com.minhdan.english_practice_backend.dto.request.SyncLessonRequest;
import com.minhdan.english_practice_backend.dto.response.LessonDto;
import com.minhdan.english_practice_backend.entity.User;
import com.minhdan.english_practice_backend.service.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    /**
     * Sync lessons from client (upsert).
     * Client sends all local lessons, server merges and returns full list.
     */
    @PostMapping("/sync")
    public ResponseEntity<List<LessonDto>> syncLessons(
            @AuthenticationPrincipal User currentUser,
            @RequestBody List<SyncLessonRequest> requests) {
        List<LessonDto> lessons = lessonService.syncLessons(currentUser, requests);
        return ResponseEntity.ok(lessons);
    }

    /**
     * Get all saved lessons for the current user.
     */
    @GetMapping
    public ResponseEntity<List<LessonDto>> getLessons(@AuthenticationPrincipal User currentUser) {
        List<LessonDto> lessons = lessonService.getUserLessons(currentUser);
        return ResponseEntity.ok(lessons);
    }

    /**
     * Delete a specific lesson.
     */
    @DeleteMapping("/{lessonId}")
    public ResponseEntity<Void> deleteLesson(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String lessonId) {
        lessonService.deleteLesson(currentUser, lessonId);
        return ResponseEntity.noContent().build();
    }
}
