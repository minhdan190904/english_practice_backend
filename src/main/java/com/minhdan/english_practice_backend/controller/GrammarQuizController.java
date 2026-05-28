package com.minhdan.english_practice_backend.controller;

import com.minhdan.english_practice_backend.dto.request.SyncGrammarMarksRequest;
import com.minhdan.english_practice_backend.dto.request.SyncGrammarQuizRequest;
import com.minhdan.english_practice_backend.dto.response.GrammarQuizDto;
import com.minhdan.english_practice_backend.entity.User;
import com.minhdan.english_practice_backend.service.GrammarQuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/grammar-quiz")
@RequiredArgsConstructor
public class GrammarQuizController {

    private final GrammarQuizService grammarQuizService;

    /**
     * Get quiz questions for a specific topic.
     */
    @GetMapping("/{topicId}")
    public ResponseEntity<GrammarQuizDto> getQuiz(@PathVariable Integer topicId) {
        GrammarQuizDto quiz = grammarQuizService.getQuizByTopicId(topicId);
        return ResponseEntity.ok(quiz);
    }

    /**
     * Get all completed topic IDs for the current user.
     */
    @GetMapping("/completed")
    public ResponseEntity<List<Integer>> getCompletedTopics(@AuthenticationPrincipal User currentUser) {
        List<Integer> completedTopics = grammarQuizService.getCompletedTopics(currentUser);
        return ResponseEntity.ok(completedTopics);
    }

    /**
     * Mark a single topic as completed.
     */
    @PostMapping("/complete/{topicId}")
    public ResponseEntity<Void> markTopicCompleted(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Integer topicId) {
        grammarQuizService.markTopicCompleted(currentUser, topicId);
        return ResponseEntity.ok().build();
    }

    /**
     * Sync completed topics from client. Upserts and returns merged list.
     */
    @PostMapping("/sync")
    public ResponseEntity<List<Integer>> syncCompletedTopics(
            @AuthenticationPrincipal User currentUser,
            @RequestBody SyncGrammarQuizRequest request) {
        List<Integer> completedTopics = grammarQuizService.syncCompletedTopics(currentUser, request.getCompletedTopicIds());
        return ResponseEntity.ok(completedTopics);
    }

    // ─── Lesson Marks (read/studied checkbox) ────────────

    /**
     * Get all lesson marks for the current user.
     */
    @GetMapping("/marks")
    public ResponseEntity<Map<Integer, Boolean>> getLessonMarks(
            @AuthenticationPrincipal User currentUser) {
        Map<Integer, Boolean> marks = grammarQuizService.getLessonMarks(currentUser);
        return ResponseEntity.ok(marks);
    }

    /**
     * Sync lesson marks from client. Upserts and returns merged map.
     */
    @PostMapping("/marks/sync")
    public ResponseEntity<Map<Integer, Boolean>> syncLessonMarks(
            @AuthenticationPrincipal User currentUser,
            @RequestBody SyncGrammarMarksRequest request) {
        // Convert String keys to Integer keys
        Map<Integer, Boolean> marks = new HashMap<>();
        if (request.getMarks() != null) {
            request.getMarks().forEach((key, value) -> marks.put(Integer.parseInt(key), value));
        }
        Map<Integer, Boolean> result = grammarQuizService.syncLessonMarks(currentUser, marks);
        return ResponseEntity.ok(result);
    }
}
