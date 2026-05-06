package com.minhdan.english_practice_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private final RestClient restClient;
    private final GoogleCredentials credentials;
    private final String projectId = "gen-lang-client-0619494454";
    private final String location = "us-central1";

    public AiService(@Value("classpath:gcp-service-account.json") Resource gcpResource) throws Exception {
        this.restClient = RestClient.create();
        try (InputStream is = gcpResource.getInputStream()) {
            this.credentials = GoogleCredentials.fromStream(is)
                    .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
        }
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
                Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", prompt))
                )
            ),
            "generationConfig", Map.of(
                "responseMimeType", "application/json",
                "responseSchema", Map.of(
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

        try {
            credentials.refreshIfExpired();
            String accessToken = credentials.getAccessToken().getTokenValue();

            String response = restClient.post()
                    .uri("https://" + location + "-aiplatform.googleapis.com/v1/projects/" + projectId + "/locations/" + location + "/publishers/google/models/gemini-2.5-flash-lite:generateContent")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> respMap = mapper.readValue(response, Map.class);
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) respMap.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate lesson via Vertex AI", e);
        }
    }
}
