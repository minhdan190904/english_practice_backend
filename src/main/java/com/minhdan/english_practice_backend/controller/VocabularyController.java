package com.minhdan.english_practice_backend.controller;

import com.minhdan.english_practice_backend.dto.request.SyncVocabularyRequest;
import com.minhdan.english_practice_backend.dto.response.UserVocabularyDto;
import com.minhdan.english_practice_backend.entity.User;
import com.minhdan.english_practice_backend.service.VocabularyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
