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

        String prompt = "You are an expert English teacher creating a personalized lesson.\n" +
                "A student at level " + level + " entered the following words/topic as their focus:\n\n" +
                "USER INPUT: \"" + inputText + "\"\n\n" +
                "STRICT INSTRUCTIONS:\n" +
                "1. The words or phrases in the USER INPUT are the CORE FOCUS of this lesson. " +
                "   You MUST naturally weave ALL the key words/phrases from the input into the passage. " +
                "   The passage must feel like it was written specifically around those words.\n" +
                "2. Write a NEW engaging English reading passage of 120-160 words at " + level + " level. " +
                "   The passage must flow naturally and tell a coherent short story or explain a concept.\n" +
                "3. Write in PURE PLAIN TEXT only — no asterisks (**), no markdown, no bullet points, no special characters. " +
                "   Use only letters, spaces, commas, periods, question marks, and exclamation marks.\n" +
                "4. Select 5-7 vocabulary words FROM THE PASSAGE that match " + level + " difficulty. " +
                "   PRIORITIZE words that came from the user's input. " +
                "   DO NOT select: stop words (a, the, is, are, have, go, do, say, make, know, get) " +
                "   and DO NOT select words already learned: [" + learnedWordsStr + "].\n" +
                "5. For each word provide its meaning IN THE CONTEXT it appears in the passage.\n" +
                "6. Provide a natural, fluent Vietnamese translation of the full passage.\n" +
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
                    .uri("https://" + location + "-aiplatform.googleapis.com/v1/projects/" + projectId + "/locations/" + location + "/publishers/google/models/gemini-2.5-flash:generateContent")
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
            prompt = "You are an expert English teacher. Extract vocabulary from this text for a " + level + " level student.\n" +
                "STRICT RULES: Do NOT modify the passage text at all. No markdown (**), no asterisks, no special formatting.\n" +
                "Provide:\n" +
                "- title: a concise, descriptive title that reflects the text's main theme\n" +
                "- passage: the EXACT custom text provided, word-for-word (no edits, no markdown)\n" +
                "- passageVi: a natural, fluent Vietnamese translation of the passage\n" +
                "- vocabulary: 5-8 challenging words for " + level + " level, each with:\n" +
                "  * word: the vocabulary word (as it appears in the text)\n" +
                "  * meaning: clear English definition in 1 sentence\n" +
                "  * meaningVi: concise Vietnamese meaning (1-4 words, e.g. 'buồng tàu', 'thói quen')\n" +
                "  * pronunciation: IPA phonetic notation (e.g. /ˈkæbɪn/)\n" +
                "  * example: the sentence from the text where this word appears\n\n" +
                "Text to analyze:\n" + customText;
        } else {
            prompt = "You are an expert English teacher creating a reading lesson.\n" +
                "TOPIC/CATEGORY: '" + topic + "'\n" +
                "STUDENT LEVEL: " + level + "\n\n" +
                "STRICT INSTRUCTIONS:\n" +
                "1. Write an engaging English reading passage of 120-160 words STRICTLY about '" + topic + "'. " +
                "   The passage must be deeply focused on this topic — every sentence should relate to '" + topic + "'. " +
                "   Include realistic details, specific vocabulary, and vivid descriptions relevant to '" + topic + "'.\n" +
                "2. Write in PURE PLAIN TEXT only — absolutely no asterisks (**), no markdown, no bullet points. " +
                "   Use only letters, spaces, commas, periods, question marks, and exclamation marks.\n" +
                "3. The passage should read like a natural, interesting mini-article or story about '" + topic + "'.\n" +
                "4. Extract 5-7 vocabulary words from the passage appropriate for " + level + " difficulty, each with:\n" +
                "   * word: the vocabulary word as it appears in the passage\n" +
                "   * meaning: clear English definition in 1 sentence\n" +
                "   * meaningVi: concise Vietnamese meaning (1-4 words)\n" +
                "   * pronunciation: IPA phonetic notation (e.g. /ˈkæbɪn/)\n" +
                "   * example: the exact sentence from the passage containing this word\n" +
                "5. Provide a natural, fluent Vietnamese translation of the full passage.";
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
                    .uri("https://" + location + "-aiplatform.googleapis.com/v1/projects/" + projectId + "/locations/" + location + "/publishers/google/models/gemini-2.5-flash:generateContent")
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
