package com.minhdan.english_practice_backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minhdan.english_practice_backend.model.VocabularyWord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;

/**
 * Indexes all Oxford category JSON words into a fast HashMap so AI-generated
 * vocabulary words can be enriched with phonetic URLs and level info.
 */
@Slf4j
@Service
public class OxfordLookupService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    // word.toLowerCase() → VocabularyWord
    private final Map<String, VocabularyWord> index = new HashMap<>();

    private static final List<String> CATEGORIES = List.of(
        "business", "culture", "daily", "education", "environment",
        "family", "food", "health", "home", "nature", "people",
        "science", "society", "sports", "technology", "transport",
        "travel", "work", "uncategorized"
    );

    @PostConstruct
    public void buildIndex() {
        int total = 0;
        for (String category : CATEGORIES) {
            try {
                ClassPathResource resource = new ClassPathResource("json/category_words/" + category + ".json");
                if (!resource.exists()) continue;
                try (InputStream is = resource.getInputStream()) {
                    List<VocabularyWord> words = objectMapper.readValue(is, new TypeReference<>() {});
                    for (VocabularyWord w : words) {
                        if (w.getWord() != null) {
                            // Don't overwrite if already indexed (prefer more specific category over uncategorized)
                            index.putIfAbsent(w.getWord().toLowerCase(), w);
                            total++;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to index category: {}", category, e);
            }
        }
        log.info("OxfordLookupService: indexed {} words across {} categories", index.size(), CATEGORIES.size());
    }

    /**
     * Look up a word and return its Oxford entry if found.
     */
    public Optional<VocabularyWord> findWord(String word) {
        if (word == null) return Optional.empty();
        return Optional.ofNullable(index.get(word.toLowerCase().trim()));
    }

    /**
     * Try to find the BASE (lemma) form of an inflected word in Oxford.
     * Handles: -ing gerunds, -ed past tense, -s plural/3rd-person, -ies plurals.
     *
     * Examples:
     *   "building"    → "build"  (verb, meaning "xây dựng")
     *   "communities" → "community"
     *   "learned"     → "learn"
     *   "running"     → "run"
     *   "making"      → "make"
     */
    public Optional<VocabularyWord> findLemma(String inflectedWord) {
        if (inflectedWord == null) return Optional.empty();
        String w = inflectedWord.toLowerCase().trim();

        // ── -ing (gerund / present participle) ──────────────────────────
        if (w.endsWith("ing") && w.length() > 5) {
            String stem = w.substring(0, w.length() - 3); // "building" → "build"
            // Direct stem: build, learn, work ...
            if (index.containsKey(stem))        return Optional.of(index.get(stem));
            // Stem + e: mak→make, writ→write, danc→dance
            if (index.containsKey(stem + "e"))  return Optional.of(index.get(stem + "e"));
            // Double-consonant: runn→run, sitt→sit, swimm→swim
            if (stem.length() >= 2 && stem.charAt(stem.length() - 1) == stem.charAt(stem.length() - 2)) {
                String dedup = stem.substring(0, stem.length() - 1);
                if (index.containsKey(dedup)) return Optional.of(index.get(dedup));
            }
        }

        // ── -ed (past tense / past participle) ──────────────────────────
        if (w.endsWith("ed") && w.length() > 4) {
            String stem = w.substring(0, w.length() - 2); // "learned" → "learn"
            if (index.containsKey(stem))        return Optional.of(index.get(stem));
            if (index.containsKey(stem + "e"))  return Optional.of(index.get(stem + "e")); // "liked"→"like"
            if (stem.length() >= 2 && stem.charAt(stem.length() - 1) == stem.charAt(stem.length() - 2)) {
                String dedup = stem.substring(0, stem.length() - 1); // "stopped" → "stop"
                if (index.containsKey(dedup)) return Optional.of(index.get(dedup));
            }
        }

        // ── -ies plural → -y ────────────────────────────────────────────
        if (w.endsWith("ies") && w.length() > 4) {
            String yForm = w.substring(0, w.length() - 3) + "y"; // "communities" → "community"
            if (index.containsKey(yForm)) return Optional.of(index.get(yForm));
        }

        // ── -s plural / 3rd-person ───────────────────────────────────────
        if (w.endsWith("s") && w.length() > 4 && !w.endsWith("ss")) {
            String stem = w.substring(0, w.length() - 1); // "bridges" → "bridge"
            if (index.containsKey(stem)) return Optional.of(index.get(stem));
        }

        return Optional.empty();
    }
}
