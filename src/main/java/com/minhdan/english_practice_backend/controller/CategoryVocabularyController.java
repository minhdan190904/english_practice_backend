package com.minhdan.english_practice_backend.controller;

import com.minhdan.english_practice_backend.dto.response.CategorySummaryResponse;
import com.minhdan.english_practice_backend.service.CategoryVocabularyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryVocabularyController {

    private final CategoryVocabularyService categoryVocabularyService;

    @GetMapping
    public ResponseEntity<List<CategorySummaryResponse>> getCategories() {
        return ResponseEntity.ok(categoryVocabularyService.getCategories());
    }

    @GetMapping("/{id}/words")
    public ResponseEntity<Object> getCategoryWords(@PathVariable String id) {
        return ResponseEntity.ok(categoryVocabularyService.getCategoryWords(id));
    }
}
