package com.minhdan.english_practice_backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minhdan.english_practice_backend.dto.SelectedWordDto;
import com.minhdan.english_practice_backend.model.VocabularyWord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SampleWordSelectorService {

    private final ObjectMapper objectMapper;
    private final Random random;

    public SampleWordSelectorService() {
        this.objectMapper = new ObjectMapper();
        this.random = new Random();
    }

    public List<SelectedWordDto> selectWords(String category, String level) {
        List<VocabularyWord> words = loadWordsForCategory(category);

        // Merge with fallback categories if not enough words
        if (words.size() < 10) {
            List<VocabularyWord> daily = loadWordsForCategory("daily");
            words = new ArrayList<>(words);
            words.addAll(daily);
        }
        if (words.size() < 5) {
            List<VocabularyWord> uncategorized = loadWordsForCategory("uncategorized");
            words.addAll(uncategorized);
        }

        List<String> allowedLevels = getAllowedLevels(level);
        log.info("Selecting words for category={}, level={}, allowedLevels={}, totalPool={}",
                category, level, allowedLevels, words.size());

        List<VocabularyWord> candidates = words.stream()
                .filter(w -> w.getLevel() != null && allowedLevels.contains(w.getLevel().toUpperCase()))
                .filter(w -> w.getSenses() != null && !w.getSenses().isEmpty())
                .filter(w -> w.getSenses().get(0).getDefinition() != null)
                .distinct()
                .collect(Collectors.toList());

        // If still not enough after level filter, open up all levels
        if (candidates.size() < 5) {
            log.warn("Not enough words for level filter, expanding to all levels. candidates={}", candidates.size());
            candidates = words.stream()
                .filter(w -> w.getSenses() != null && !w.getSenses().isEmpty()
                        && w.getSenses().get(0).getDefinition() != null)
                .distinct()
                .collect(Collectors.toList());
        }

        Collections.shuffle(candidates);

        int targetCount = 5 + random.nextInt(3); // 5 to 7 words
        List<VocabularyWord> selected = candidates.stream()
                .limit(targetCount)
                .collect(Collectors.toList());

        log.info("Selected {} words: {}", selected.size(),
                selected.stream().map(VocabularyWord::getWord).collect(Collectors.joining(", ")));

        return selected.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    private List<VocabularyWord> loadWordsForCategory(String category) {
        try {
            ClassPathResource resource = new ClassPathResource("json/category_words/" + category.toLowerCase() + ".json");
            if (!resource.exists()) {
                log.warn("Category file not found: {}.json", category);
                return new ArrayList<>();
            }
            try (InputStream is = resource.getInputStream()) {
                return objectMapper.readValue(is, new TypeReference<List<VocabularyWord>>() {});
            }
        } catch (Exception e) {
            log.error("Failed to load vocabulary data for category: {}", category, e);
            return new ArrayList<>();
        }
    }

    private List<String> getAllowedLevels(String level) {
        if (level == null) return Arrays.asList("A1", "A2", "B1", "B2", "C1", "C2");
        String l = level.toUpperCase();
        switch (l) {
            case "A1": return Arrays.asList("A1", "A2");
            case "A2": return Arrays.asList("A1", "A2", "B1");
            case "B1": return Arrays.asList("A2", "B1", "A1");
            case "B2": return Arrays.asList("B1", "B2", "A2");
            case "C1": return Arrays.asList("B2", "C1", "B1");
            case "C2": return Arrays.asList("C1", "C2", "B2");
            default:   return Arrays.asList("A1", "A2", "B1", "B2", "C1", "C2");
        }
    }

    private SelectedWordDto mapToDto(VocabularyWord word) {
        SelectedWordDto dto = new SelectedWordDto();
        dto.setWord(word.getWord());
        dto.setLevel(word.getLevel());
        dto.setCategory(word.getCategory());
        dto.setPos(word.getPos());
        dto.setPhoneticText(word.getPhoneticText());
        dto.setPhoneticAmText(word.getPhoneticAmText());
        
        if (word.getSenses() != null && !word.getSenses().isEmpty()) {
            dto.setDefinition(word.getSenses().get(0).getDefinition());
            if (word.getSenses().get(0).getExamples() != null && !word.getSenses().get(0).getExamples().isEmpty()) {
                dto.setExample(word.getSenses().get(0).getExamples().get(0).getX());
            }
        }
        return dto;
    }
}
