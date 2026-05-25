package com.minhdan.english_practice_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class VertexImageService {

    private final RestClient restClient;
    private final GoogleCredentials credentials;
    private final String projectId = "gen-lang-client-0619494454";
    private final String location = "us-central1";
    // Imagen 3 Fast — "banana 1" (imagegeneration@006 is EOL, migrated to imagen-3.0-fast-generate-001)
    private static final String MODEL = "imagen-3.0-fast-generate-001";

    public VertexImageService(@Value("classpath:gcp-service-account.json") Resource gcpResource) throws Exception {
        this.restClient = RestClient.create();
        try (InputStream is = gcpResource.getInputStream()) {
            this.credentials = GoogleCredentials.fromStream(is)
                    .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
        }
    }

    /**
     * Generate an illustration image for the given English lesson passage.
     * Uses Imagen 3 Fast (imagegeneration@006) on Vertex AI.
     *
     * @param title    The lesson title (e.g. "A Walk in the Forest")
     * @param passage  The lesson passage text (used to extract scene keywords)
     * @return base64-encoded PNG string, or null on any error (graceful fallback)
     */
    public String generateImage(String title, String passage) {
        try {
            String prompt = buildPrompt(title);

            Map<String, Object> requestBody = Map.of(
                "instances", List.of(
                    Map.of("prompt", prompt)
                ),
                "parameters", Map.of(
                    "sampleCount", 1,
                    "aspectRatio", "16:9",
                    "safetySetting", "block_few",
                    "personGeneration", "allow_adult"
                )
            );

            credentials.refreshIfExpired();
            String accessToken = credentials.getAccessToken().getTokenValue();

            String endpoint = "https://" + location + "-aiplatform.googleapis.com/v1/projects/"
                    + projectId + "/locations/" + location
                    + "/publishers/google/models/" + MODEL + ":predict";

            String response = restClient.post()
                    .uri(endpoint)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            log.info("Imagen raw response (first 500 chars): {}", response != null && response.length() > 500 ? response.substring(0, 500) : response);

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> respMap = mapper.readValue(response, Map.class);

            // Check for RAI / safety filter rejection
            Object raiReason = respMap.get("raiFilteredReason");
            if (raiReason != null) {
                log.warn("Imagen blocked by safety filter for title: {} — reason: {}", title, raiReason);
                return null;
            }

            List<Map<String, Object>> predictions = (List<Map<String, Object>>) respMap.get("predictions");
            if (predictions == null || predictions.isEmpty()) {
                log.warn("Imagen returned no predictions for title: {} — full keys: {}", title, respMap.keySet());
                return null;
            }

            // Each prediction may itself have a raiFilteredReason
            Map<String, Object> pred = predictions.get(0);
            Object predRai = pred.get("raiFilteredReason");
            if (predRai != null) {
                log.warn("Imagen prediction blocked by safety filter for title: {} — reason: {}", title, predRai);
                return null;
            }

            String base64 = (String) pred.get("bytesBase64Encoded");
            if (base64 == null || base64.isBlank()) {
                log.warn("Imagen returned empty base64 for title: {} — prediction keys: {}", title, pred.keySet());
                return null;
            }

            log.info("Image generated successfully for lesson: {} ({} chars base64)", title, base64.length());
            return base64;

        } catch (Exception e) {
            log.warn("Image generation failed (non-critical), lesson will continue without image: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Build a safe, clean prompt using only the lesson title.
     * Avoids dumping raw passage text which may contain words that trigger safety filters.
     */
    private String buildPrompt(String title) {
        String safeTitle = (title != null) ? title.replaceAll("[^a-zA-Z0-9 ',.-]", " ").trim() : "English lesson";

        return "A vivid, educational illustration for an English language learning app. "
                + "Topic: \"" + safeTitle + "\". "
                + "Style: modern digital art, bright friendly colors, clean composition, "
                + "cinematic wide shot, photorealistic or stylized painting. "
                + "No text, no letters, no words, no numbers, no logos, no watermarks.";
    }
}

