package com.minhdan.english_practice_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minhdan.english_practice_backend.dto.GenerateLessonRequest;
import com.minhdan.english_practice_backend.dto.request.GenerateLessonFromInputRequest;
import com.minhdan.english_practice_backend.model.Sense;
import com.minhdan.english_practice_backend.model.VocabularyWord;
import com.minhdan.english_practice_backend.service.AiService;
import com.minhdan.english_practice_backend.service.OxfordLookupService;
import com.minhdan.english_practice_backend.service.TelegramService;
import com.minhdan.english_practice_backend.service.VertexImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final OxfordLookupService oxfordLookup;
    private final VertexImageService vertexImageService;
    private final TelegramService telegramService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Common English stop words — never highlight these
    private static final Set<String> STOP_WORDS = Set.of(
        "a","an","the","is","are","was","were","be","been","being","have","has","had",
        "do","does","did","will","would","could","should","may","might","shall","can",
        "to","of","in","for","on","with","at","by","from","up","about","into","through",
        "during","before","after","above","below","between","or","and","but","if","as",
        "until","while","since","so","yet","both","not","only","just","very","too","also",
        "i","me","my","we","our","you","your","he","she","it","they","them","their",
        "this","that","these","those","what","which","who","how","when","where","why",
        "all","more","most","no","its","his","her","one","two","three","there",
        "then","than","such","each","other","any","some","own","same","new",
        "now","out","here","back","even","well","way","get","got","said","says","say",
        "go","goes","went","come","came","know","knew","think","thought","see","saw",
        "make","made","take","took","give","gave","use","used","want","like","look",
        "first","last","long","great","little","old","right","big","high","next"
    );

    @PostMapping("/generate-lesson")
    public ResponseEntity<String> generateLesson(@RequestBody GenerateLessonRequest request) {
        long t0 = System.currentTimeMillis();
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📝 [LESSON] START  topic='{}' level='{}'", request.getTopic(), request.getLevel());

        long t1 = System.currentTimeMillis();
        String raw = aiService.generateLesson(request.getTopic(), request.getLevel(), request.getCustomText());
        long passageMs = System.currentTimeMillis() - t1;
        log.info("🤖 [STEP 1] Passage generated  → {}ms", passageMs);

        long[] timings = new long[3]; // [vocabMs, imageMs, waitMs]
        String result = enrichAndClean(raw, request.getLevel(), timings);
        long totalMs = System.currentTimeMillis() - t0;
        log.info("✅ [LESSON] DONE   total        → {}ms", totalMs);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Send timing report to Telegram (async, non-blocking)
        final String topic = request.getTopic();
        final String level = request.getLevel();
        CompletableFuture.runAsync(() -> telegramService.sendMessage(
            "<b>📚 AI Lesson Generated</b>\n" +
            "━━━━━━━━━━━━━━━━━━━━━━\n" +
            "📌 Topic: <b>" + topic + "</b>\n" +
            "🎯 Level: <b>" + level + "</b>\n" +
            "━━━━━━━━━━━━━━━━━━━━━━\n" +
            "🤖 Step 1 - Passage (Gemini 2.5 Flash): <b>" + passageMs + "ms</b>\n" +
            "📖 Step 3A - Oxford Vocab Lookup:        <b>" + timings[0] + "ms</b>\n" +
            "🖼 Step 3B - Image (Imagen 3 Fast):      <b>" + timings[1] + "ms</b>\n" +
            "⏳ Step 4  - Wait for image future:      <b>+" + timings[2] + "ms</b>\n" +
            "━━━━━━━━━━━━━━━━━━━━━━\n" +
            "✅ Total: <b>" + totalMs + "ms</b> (~" + (totalMs / 1000) + "s)"
        ));

        return ResponseEntity.ok(result);
    }

    @PostMapping("/generate-lesson-from-input")
    public ResponseEntity<String> generateLessonFromInput(@RequestBody GenerateLessonFromInputRequest request) {
        long t0 = System.currentTimeMillis();
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📝 [LESSON-INPUT] START  level='{}'", request.getLevel());

        String level = request.getLevel() != null ? request.getLevel() : "B1";
        long t1 = System.currentTimeMillis();
        String raw = aiService.generateLessonFromInput(request.getInputText(), level, request.getLearnedWords());
        long passageMs = System.currentTimeMillis() - t1;
        log.info("🤖 [STEP 1] Passage generated  → {}ms", passageMs);

        long[] timings = new long[3]; // [vocabMs, imageMs, waitMs]
        String result = enrichAndClean(raw, level, timings);
        long totalMs = System.currentTimeMillis() - t0;
        log.info("✅ [LESSON-INPUT] DONE   total  → {}ms", totalMs);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Send timing report to Telegram (async, non-blocking)
        final String inputPreview = request.getInputText() != null
                ? request.getInputText().substring(0, Math.min(50, request.getInputText().length()))
                : "(empty)";
        CompletableFuture.runAsync(() -> telegramService.sendMessage(
            "<b>📚 AI Lesson (From Input) Generated</b>\n" +
            "━━━━━━━━━━━━━━━━━━━━━━\n" +
            "✏️ Input: <i>" + inputPreview + "...</i>\n" +
            "🎯 Level: <b>" + level + "</b>\n" +
            "━━━━━━━━━━━━━━━━━━━━━━\n" +
            "🤖 Step 1 - Passage (Gemini 2.5 Flash): <b>" + passageMs + "ms</b>\n" +
            "📖 Step 3A - Oxford Vocab Lookup:        <b>" + timings[0] + "ms</b>\n" +
            "🖼 Step 3B - Image (Imagen 3 Fast):      <b>" + timings[1] + "ms</b>\n" +
            "⏳ Step 4  - Wait for image future:      <b>+" + timings[2] + "ms</b>\n" +
            "━━━━━━━━━━━━━━━━━━━━━━\n" +
            "✅ Total: <b>" + totalMs + "ms</b> (~" + (totalMs / 1000) + "s)"
        ));

        return ResponseEntity.ok(result);
    }

    /**
     * 1. Strip ** markdown from passage text
     * 2. Replace AI-selected vocab with pure Oxford algorithm
     * 3. Generate illustration image via Imagen 3 Fast
     *    — kicked off BEFORE vocab selection so both run in parallel.
     * timings: long[3] — [0]=vocabMs, [1]=imageMs, [2]=waitMs (written by this method)
     */
    @SuppressWarnings("unchecked")
    private String enrichAndClean(String jsonStr, String level, long[] timings) {
        try {
            Map<String, Object> lesson = objectMapper.readValue(jsonStr, Map.class);

            // --- 1. Strip ** markdown ---
            if (lesson.containsKey("passage")) {
                lesson.put("passage", stripMarkdown((String) lesson.get("passage")));
            }
            if (lesson.containsKey("passageVi")) {
                lesson.put("passageVi", stripMarkdown((String) lesson.get("passageVi")));
            }

            // Strip markdown from sentences
            if (lesson.containsKey("sentences")) {
                List<Map<String, Object>> sentences = (List<Map<String, Object>>) lesson.get("sentences");
                if (sentences != null) {
                    for (Map<String, Object> s : sentences) {
                        if (s.containsKey("en")) s.put("en", stripMarkdown((String) s.get("en")));
                        if (s.containsKey("vi")) s.put("vi", stripMarkdown((String) s.get("vi")));
                    }
                }
            }

            String passage = (String) lesson.get("passage");
            String title   = (String) lesson.get("title");

            // --- 2. Kick off image generation NOW (parallel with vocab selection below) ---
            final String passageForImage = passage;
            final long imageStart = System.currentTimeMillis();
            CompletableFuture<String> imageFuture = (passage != null && !passage.isBlank())
                    ? CompletableFuture.supplyAsync(() -> {
                          log.info("🖼️  [STEP 3B] Image generation  STARTED  (async thread)");
                          String img = vertexImageService.generateImage(title, passageForImage);
                          if (timings != null) timings[1] = System.currentTimeMillis() - imageStart;
                          log.info("🖼️  [STEP 3B] Image generation  DONE     → {}ms", timings != null ? timings[1] : "?");
                          return img;
                      })
                    : CompletableFuture.completedFuture(null);

            // --- 3. Oxford-only vocab selection (runs concurrently with image generation) ---
            if (passage != null && !passage.isBlank()) {
                long vocabStart = System.currentTimeMillis();
                log.info("📖 [STEP 3A] Oxford vocab lookup STARTED");
                List<Map<String, Object>> oxfordVocab = selectVocabFromOxford(passage, level);
                if (timings != null) timings[0] = System.currentTimeMillis() - vocabStart;
                log.info("📖 [STEP 3A] Oxford vocab lookup DONE    → {}ms  ({} words found)",
                        timings != null ? timings[0] : "?", oxfordVocab.size());
                if (!oxfordVocab.isEmpty()) {
                    lesson.put("vocabulary", oxfordVocab);
                }
            }

            // --- 4. Join image future (may already be done by now) ---
            long joinStart = System.currentTimeMillis();
            String imageBase64 = imageFuture.join();
            long joinWait = System.currentTimeMillis() - joinStart;
            if (timings != null) timings[2] = joinWait;
            if (joinWait > 50) {
                log.info("⏳ [STEP 4]  Waited for image future  → +{}ms  (image was the bottleneck)", joinWait);
            } else {
                log.info("⚡ [STEP 4]  Image was already ready   → +{}ms  (vocab was the bottleneck)", joinWait);
            }
            if (imageBase64 != null) {
                log.info("🖼️  [STEP 4]  imageBase64 size          → {} bytes (~{}KB)",
                        imageBase64.length(), imageBase64.length() / 1024);
                lesson.put("imageBase64", imageBase64);
            } else {
                log.warn("⚠️  [STEP 4]  imageBase64 is NULL — image generation may have failed");
            }

            return objectMapper.writeValueAsString(lesson);
        } catch (Exception e) {
            log.warn("enrichAndClean: parse/enrich failed, returning stripped raw: {}", e.getMessage());
            return jsonStr.replace("**", "");
        }
    }

    /**
     * Pure algorithm: scan every word in the passage, look it up in the Oxford index
     * (with lemmatization for inflected forms), filter by level, pick up to 7 words.
     *
     * Key: we keep the PASSAGE FORM as the display word (for highlighting in Flutter)
     * but use the BASE FORM's meaning/pronunciation/examples (for correct context).
     *
     * Example: "Building bridges" → display word = "Building", meaning = "build" (verb) = "xây dựng" ✅
     */
    private List<Map<String, Object>> selectVocabFromOxford(String passage, String level) {
        List<String> targetLevels = getTargetLevels(level);
        String[] tokens = passage.split("[^a-zA-Z]+");

        LinkedHashSet<String> seen = new LinkedHashSet<>();
        // Each entry: [0]=passageForm (string), [1]=resolvedWord (VocabularyWord)
        List<Object[]> candidates    = new ArrayList<>(); // level-matched
        List<Object[]> fallback      = new ArrayList<>(); // any Oxford match

        for (String token : tokens) {
            String word = token.toLowerCase().trim();
            if (word.length() < 4) continue;
            if (STOP_WORDS.contains(word)) continue;
            if (seen.contains(word)) continue;
            seen.add(word);

            // 1. Try exact match
            Optional<VocabularyWord> exactOpt = oxfordLookup.findWord(word);
            // 2. Try lemma (base form) — resolves inflected forms
            Optional<VocabularyWord> lemmaOpt = oxfordLookup.findLemma(word);

            // Decide which VocabularyWord to use for meaning:
            // Prefer lemma (base form) when:
            //   a) exact form not found in Oxford, OR
            //   b) word is inflected (-ing/-ed/-s) and lemma has verb/adj POS
            VocabularyWord resolved;
            if (exactOpt.isEmpty() && lemmaOpt.isEmpty()) continue;
            if (exactOpt.isEmpty()) {
                resolved = lemmaOpt.get();
            } else if (lemmaOpt.isEmpty()) {
                resolved = exactOpt.get();
            } else {
                // Both found — prefer lemma if the passage word is inflected and lemma is a verb
                boolean isInflected = word.endsWith("ing") || word.endsWith("ed") || word.endsWith("s");
                String lemmaPos = lemmaOpt.get().getPos();
                boolean lemmaIsVerb = lemmaPos != null && lemmaPos.toLowerCase().contains("verb");
                resolved = (isInflected && lemmaIsVerb) ? lemmaOpt.get() : exactOpt.get();
            }

            // Must have at least one sense with definition
            if (resolved.getSenses() == null || resolved.getSenses().isEmpty()) continue;
            if (resolved.getSenses().get(0).getDefinition() == null) continue;

            // passageForm = original token from passage (capitalization preserved), used for highlighting
            Object[] entry = new Object[]{token, resolved};
            fallback.add(entry);
            if (resolved.getLevel() != null && targetLevels.contains(resolved.getLevel().toUpperCase())) {
                candidates.add(entry);
            }
        }

        List<Object[]> selected = candidates.size() >= 3 ? candidates : fallback;
        Collections.shuffle(selected, new Random());
        selected = selected.stream().limit(7).collect(Collectors.toList());

        log.info("Oxford vocab selection: {} candidates (level-matched={}), selected={}",
                fallback.size(), candidates.size(),
                selected.stream().map(e -> (String) e[0]).collect(Collectors.joining(", ")));

        return selected.stream()
                .map(e -> toVocabMap((String) e[0], (VocabularyWord) e[1]))
                .collect(Collectors.toList());
    }

    /** Build vocab map using passage word form (for Flutter highlighting) but base word's meanings */
    private Map<String, Object> toVocabMap(String passageForm, VocabularyWord w) {
        Map<String, Object> v = new LinkedHashMap<>();
        // Use the passage form (e.g. "building") — Flutter highlights by this word
        v.put("word", passageForm.toLowerCase());

        Sense sense = (w.getSenses() != null && !w.getSenses().isEmpty()) ? w.getSenses().get(0) : null;
        v.put("meaning",       sense != null && sense.getDefinition()    != null ? sense.getDefinition()    : "");
        v.put("meaningVi",     sense != null && sense.getShortMeaningVi()!= null ? sense.getShortMeaningVi(): "");
        v.put("definitionVi",  sense != null && sense.getDefinitionVi()  != null ? sense.getDefinitionVi()  : "");
        v.put("pronunciation", w.getPhoneticText()  != null ? w.getPhoneticText()  : "");
        v.put("phoneticAmText",w.getPhoneticAmText()!= null ? w.getPhoneticAmText(): "");
        v.put("phoneticUrl",   w.getPhonetic()      != null ? w.getPhonetic()      : "");
        v.put("phoneticAmUrl", w.getPhoneticAm()    != null ? w.getPhoneticAm()    : "");
        v.put("pos",           w.getPos()           != null ? w.getPos()           : "");

        String example = "";
        if (sense != null && sense.getExamples() != null && !sense.getExamples().isEmpty()) {
            example = sense.getExamples().get(0).getX() != null ? sense.getExamples().get(0).getX() : "";
        }
        v.put("example", example);
        return v;
    }

    private List<String> getTargetLevels(String level) {
        if (level == null || level.isBlank()) return List.of("A1","A2","B1","B2","C1","C2");
        
        // Extract just the level code (e.g. "B1 - Intermediate" -> "B1")
        String code = level.trim().toUpperCase();
        if (code.contains("-")) {
            code = code.split("-")[0].trim();
        } else if (code.contains(" ")) {
            code = code.split(" ")[0].trim();
        }

        switch (code) {
            case "A1": return List.of("A1","A2");
            case "A2": return List.of("A1","A2","B1");
            case "B1": return List.of("A2","B1","B2");
            case "B2": return List.of("B1","B2","C1");
            case "C1": return List.of("B2","C1","C2");
            case "C2": return List.of("C1","C2");
            default:   return List.of("A1","A2","B1","B2","C1","C2");
        }
    }

    private String stripMarkdown(String text) {
        if (text == null) return null;
        return text.replaceAll("\\*\\*", "").replaceAll("\\*", "");
    }
}
