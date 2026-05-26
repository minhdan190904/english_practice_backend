package com.minhdan.english_practice_backend.controller;

import com.minhdan.english_practice_backend.dto.request.SyncVocabularyRequest;
import com.minhdan.english_practice_backend.dto.response.UserVocabularyDto;
import com.minhdan.english_practice_backend.entity.User;
import com.minhdan.english_practice_backend.service.VocabularyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/vocabularies")
@RequiredArgsConstructor
public class VocabularyController {

    private final VocabularyService vocabularyService;

    @PostMapping("/sync")
    public ResponseEntity<List<UserVocabularyDto>> syncVocabularies(
            @AuthenticationPrincipal User currentUser,
            @RequestBody List<SyncVocabularyRequest> requests) {
        
        List<UserVocabularyDto> syncedVocabularies = vocabularyService.syncUserVocabularies(currentUser, requests);
        return ResponseEntity.ok(syncedVocabularies);
    }

    /**
     * Get vocabularies due for SRS review.
     */
    @GetMapping("/due")
    public ResponseEntity<List<UserVocabularyDto>> getDueVocabularies(
            @AuthenticationPrincipal User currentUser) {
        List<UserVocabularyDto> dueWords = vocabularyService.getDueForReview(currentUser);
        return ResponseEntity.ok(dueWords);
    }

    /**
     * Submit SRS review result for a word.
     */
    @PostMapping("/review")
    public ResponseEntity<UserVocabularyDto> submitReview(
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, Object> body) {
        String word = (String) body.get("word");
        int quality = (int) body.get("quality"); // 0-5 (SM-2 scale)
        UserVocabularyDto result = vocabularyService.submitReview(currentUser, word, quality);
        return ResponseEntity.ok(result);
    }

    /**
     * Get all vocabularies for current user.
     */
    @GetMapping
    public ResponseEntity<List<UserVocabularyDto>> getAllVocabularies(
            @AuthenticationPrincipal User currentUser) {
        List<UserVocabularyDto> vocabs = vocabularyService.getAllForUser(currentUser);
        return ResponseEntity.ok(vocabs);
    }
}
