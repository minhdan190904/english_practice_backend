package com.minhdan.english_practice_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
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
    private final String projectId;
    private final String location;

    public AiService(
            @Value("${gcp.config.path}") String gcpConfigPath,
            @Value("${gcp.project-id}") String projectId,
            @Value("${gcp.location:us-central1}") String location
    ) throws Exception {
        this.restClient = RestClient.create();
        this.projectId = projectId;
        this.location = location;

        try (java.io.InputStream is = new java.io.FileInputStream(gcpConfigPath)) {
            this.credentials = GoogleCredentials.fromStream(is)
                    .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
        }
    }

    public String generateLessonFromInput(String inputText, String level, java.util.List<String> learnedWords) {
        String learnedWordsStr = (learnedWords != null && !learnedWords.isEmpty())
                ? String.join(", ", learnedWords)
                : "(none)";

        String prompt = "You are an expert English teacher. A student at level " + level + " provided the following input:\n\n" +
                "\"" + inputText + "\"\n\n" +
                "Instructions:\n" +
                "1. Based on the topic/content of this input, write a NEW engaging English reading passage (100-150 words) appropriate for a " + level + " level student.\n" +
                "2. The passage should be thematically related to the input but written from scratch — do NOT copy the input verbatim.\n" +
                "3. From your generated passage, select 5-7 vocabulary words that are challenging for " + level + " level. " +
                "AVOID selecting extremely common words (a, the, is, are, have, go, do, say, make, know, get) and AVOID selecting any word from this list of already-learned words: [" + learnedWordsStr + "].\n" +
                "4. For each selected word, extract it IN CONTEXT (use the exact meaning it carries in the passage).\n" +
                "5. Provide a precise Vietnamese translation of the full passage.\n" +
                "Respond with a JSON object following the provided schema.";

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
                        "passageVi", Map.of("type", "string", "description", "Natural Vietnamese translation of the passage"),
                        "vocabulary", Map.of(
                            "type", "array",
                            "items", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                    "word", Map.of("type", "string"),
                                    "meaning", Map.of("type", "string", "description", "Short English definition"),
                                    "meaningVi", Map.of("type", "string", "description", "Short Vietnamese meaning, 1-4 words"),
                                    "pronunciation", Map.of("type", "string", "description", "IPA pronunciation e.g. /ˈkæbɪn/"),
                                    "example", Map.of("type", "string", "description", "Example sentence from the passage")
                                ),
                                "required", List.of("word", "meaning", "meaningVi", "pronunciation", "example")
                            )
                        )
                    ),
                    "required", List.of("title", "passage", "passageVi", "vocabulary")
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
            throw new RuntimeException("Failed to generate lesson from input via Vertex AI", e);
        }
    }

    public String generateLesson(String topic, String level, String customText) {
        String prompt;
        if (customText != null && !customText.trim().isEmpty()) {
            prompt = "Extract difficult vocabulary from the following English text for a " + level + " level student.\n" +
                "Provide:\n" +
                "- title: a short title for the text\n" +
                "- passage: the exact custom text provided\n" +
                "- passageVi: a natural, precise Vietnamese translation of the passage\n" +
                "- vocabulary: list of extracted words, each with:\n" +
                "  * word: the vocabulary word\n" +
                "  * meaning: short English definition (1 sentence)\n" +
                "  * meaningVi: short Vietnamese translation/meaning (1-4 words, e.g. 'buồng tàu', 'thói quen')\n" +
                "  * pronunciation: IPA pronunciation (e.g. /ˈkæbɪn/)\n" +
                "  * example: an example sentence from the text\n\n" +
                "Text: " + customText;
        } else {
            prompt = "Generate a short English reading passage about '" + topic + "' for a " + level + " level student.\n" +
                "Extract difficult vocabulary from the passage. Provide:\n" +
                "- title: a short title\n" +
                "- passage: the reading passage\n" +
                "- passageVi: a natural, precise Vietnamese translation of the passage\n" +
                "- vocabulary: list of extracted words, each with:\n" +
                "  * word: the vocabulary word\n" +
                "  * meaning: short English definition (1 sentence)\n" +
                "  * meaningVi: short Vietnamese translation/meaning (1-4 words, e.g. 'buồng tàu', 'thói quen')\n" +
                "  * pronunciation: IPA pronunciation (e.g. /ˈkæbɪn/)\n" +
                "  * example: an example sentence from the passage";
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
                        "passageVi", Map.of("type", "string", "description", "Natural Vietnamese translation of the passage"),
                        "vocabulary", Map.of(
                            "type", "array",
                            "items", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                    "word", Map.of("type", "string"),
                                    "meaning", Map.of("type", "string", "description", "Short English definition"),
                                    "meaningVi", Map.of("type", "string", "description", "Short Vietnamese meaning, 1-4 words"),
                                    "pronunciation", Map.of("type", "string", "description", "IPA pronunciation e.g. /ˈkæbɪn/"),
                                    "example", Map.of("type", "string", "description", "Example sentence from the passage")
                                ),
                                "required", List.of("word", "meaning", "meaningVi", "pronunciation", "example")
                            )
                        )
                    ),
                    "required", List.of("title", "passage", "passageVi", "vocabulary")
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
