package com.minhdan.english_practice_backend.service;

import com.minhdan.english_practice_backend.dto.response.CategorySummaryResponse;

import java.util.List;

public interface CategoryVocabularyService {
    List<CategorySummaryResponse> getCategories();
    Object getCategoryWords(String categoryId);
}
