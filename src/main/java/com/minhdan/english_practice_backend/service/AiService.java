package com.minhdan.english_practice_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private final RestClient restClient;
    private final String apiKey;

    public AiService(@Value("${gemini.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.create();
    }

    public String generateLesson(String topic, String level, String customText) {
        String prompt;
        if (customText != null && !customText.trim().isEmpty()) {
            prompt = "Extract difficult vocabulary from the following English text for a " + level + " level student. Provide a title for the text, the passage itself (which is the exact custom text provided), and a list of extracted vocabulary words with meaning in Vietnamese, IPA pronunciation, and an example sentence from the text.\n\nText: " + customText;
        } else {
            prompt = "Generate a short English reading passage about '" + topic + "' for a " + level + " level student. Extract difficult vocabulary from the passage. Provide a title, the passage, and a list of extracted vocabulary words with meaning in Vietnamese, IPA pronunciation, and an example sentence from the passage.";
        }

        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(Map.of("text", prompt)))
            ),
            "generationConfig", Map.of(
                "responseMimeType", "application/json",
                "responseJsonSchema", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "title", Map.of("type", "string"),
                        "passage", Map.of("type", "string"),
                        "vocabulary", Map.of(
                            "type", "array",
                            "items", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                    "word", Map.of("type", "string"),
                                    "meaning", Map.of("type", "string", "description", "Vietnamese meaning"),
                                    "pronunciation", Map.of("type", "string", "description", "IPA pronunciation"),
                                    "example", Map.of("type", "string", "description", "Example sentence from the passage")
                                ),
                                "required", List.of("word", "meaning", "pronunciation", "example")
                            )
                        )
                    ),
                    "required", List.of("title", "passage", "vocabulary")
                )
            )
        );

        String response = restClient.post()
                .uri("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent")
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> respMap = mapper.readValue(response, Map.class);
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) respMap.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini response", e);
        }
    }
}
