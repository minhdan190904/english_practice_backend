package com.minhdan.english_practice_backend.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minhdan.english_practice_backend.dto.response.CategorySummaryResponse;
import com.minhdan.english_practice_backend.service.CategoryVocabularyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryVocabularyServiceImpl implements CategoryVocabularyService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<CategorySummaryResponse> getCategories() {
        try {
            ClassPathResource resource = new ClassPathResource("json/category_words/summary.json");
            try (InputStream is = resource.getInputStream()) {
                return objectMapper.readValue(is, new TypeReference<List<CategorySummaryResponse>>() {});
            }
        } catch (IOException e) {
            log.error("Failed to load category summary", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Object getCategoryWords(String categoryId) {
        try {
            ClassPathResource resource = new ClassPathResource("json/category_words/" + categoryId + ".json");
            if (!resource.exists()) {
                throw new RuntimeException("Category not found: " + categoryId);
            }
            try (InputStream is = resource.getInputStream()) {
                // Return as an Object (parsed JSON List) so Spring can serialize it back directly
                return objectMapper.readValue(is, Object.class);
            }
        } catch (IOException e) {
            log.error("Failed to load category words for " + categoryId, e);
            throw new RuntimeException("Failed to load category words");
        }
    }
}
