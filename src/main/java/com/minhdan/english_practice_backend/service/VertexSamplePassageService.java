package com.minhdan.english_practice_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.minhdan.english_practice_backend.dto.SelectedWordDto;
import com.minhdan.english_practice_backend.dto.response.GenerateSamplePassageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VertexSamplePassageService {

    private final RestClient restClient;
    private final GoogleCredentials credentials;
    private final VertexImageService vertexImageService;
    private final String projectId = "gen-lang-client-0619494454";
    private final String location = "us-central1";

    public VertexSamplePassageService(
            @Value("classpath:gcp-service-account.json") Resource gcpResource,
            VertexImageService vertexImageService) throws Exception {
        this.restClient = RestClient.create();
        this.vertexImageService = vertexImageService;
        try (InputStream is = gcpResource.getInputStream()) {
            this.credentials = GoogleCredentials.fromStream(is)
                    .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
        }
    }

    public GenerateSamplePassageResponse generatePassage(String category, String level, int minWords, int maxWords, List<SelectedWordDto> selectedWords) {
        String wordsListStr = selectedWords.stream()
                .map(w -> w.getWord() + " - " + w.getDefinition() + " - Example: " + w.getExample())
                .collect(Collectors.joining("\n"));

        String prompt = String.format("""
                You are an expert English teacher creating short reading passages for language learners.
                Create a short English passage for a %s learner.
                Category: %s
                
                Required vocabulary words:
                %s
                
                Rules:
                - Use every required vocabulary word naturally at least once.
                - Make the passage meaningful, coherent, and interesting.
                - Use grammar suitable for level %s.
                - Do not use violent, political, sexual, or sensitive content.
                - Keep the passage between %d and %d words.
                - IMPORTANT: The passage must be pure plain text only. No markdown, no bullet points, no asterisks, no dashes, no special formatting characters of any kind. Only regular letters, spaces, commas, periods, question marks, and exclamation marks.
                - Write in flowing prose paragraphs. Do not use numbered lists or bullet lists.
                - Finally, provide a precise and natural Vietnamese translation of the passage you generated.
                """, level, category, wordsListStr, level, minWords, maxWords);

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
                        "title", Map.of("type", "string", "description", "A short attractive title"),
                        "passage", Map.of("type", "string", "description", "The generated English passage."),
                        "passageVi", Map.of("type", "string", "description", "The Vietnamese translation of the passage.")
                    ),
                    "required", List.of("title", "passage", "passageVi")
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
            String jsonOutput = (String) parts.get(0).get("text");
            
            Map<String, String> passageMap = mapper.readValue(jsonOutput, Map.class);
            
            String generatedTitle   = passageMap.get("title");
            String generatedPassage = passageMap.get("passage");

            // ── Kick off image generation immediately (parallel with remaining processing) ──
            CompletableFuture<String> imageFuture = CompletableFuture.supplyAsync(
                    () -> vertexImageService.generateImage(generatedTitle, generatedPassage)
            );

            int wordCount = generatedPassage.split("\\s+").length;
            String passageVi = passageMap.get("passageVi");

            // ── Join image future (may already be done by now) ──
            String imageBase64 = imageFuture.join();
            
            return GenerateSamplePassageResponse.builder()
                    .title(generatedTitle)
                    .passage(generatedPassage)
                    .passageVi(passageVi)
                    .selectedWords(selectedWords)
                    .category(category)
                    .level(level)
                    .wordCount(wordCount)
                    .imageBase64(imageBase64)
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate sample passage via Vertex AI", e);
            // Fallback response
            String fallbackPassage = "Welcome to our learning section. Today we will learn about " + category + ". " 
                + "It is important to understand basic concepts. Let's study the new words carefully. "
                + "Please practice them every day. " + selectedWords.stream().map(w -> "Remember the word " + w.getWord() + ". ").collect(Collectors.joining());
            
            String fallbackPassageVi = "Chào mừng đến với phần học thuật của chúng tôi. Hôm nay chúng ta sẽ học về " + category + ". "
                + "Điều quan trọng là phải hiểu các khái niệm cơ bản. Hãy nghiên cứu kỹ các từ mới. "
                + "Hãy luyện tập chúng mỗi ngày. " + selectedWords.stream().map(w -> "Hãy nhớ từ " + w.getWord() + ". ").collect(Collectors.joining());
            
            return GenerateSamplePassageResponse.builder()
                    .title("Sample " + category + " Passage")
                    .passage(fallbackPassage)
                    .passageVi(fallbackPassageVi)
                    .selectedWords(selectedWords)
                    .category(category)
                    .level(level)
                    .wordCount(fallbackPassage.split("\\s+").length)
                    .build();
        }
    }
}
