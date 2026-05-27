package com.minhdan.english_practice_backend.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minhdan.english_practice_backend.dto.request.SyncLessonRequest;
import com.minhdan.english_practice_backend.dto.response.LessonDto;
import com.minhdan.english_practice_backend.entity.User;
import com.minhdan.english_practice_backend.entity.UserLesson;
import com.minhdan.english_practice_backend.repository.UserLessonRepository;
import com.minhdan.english_practice_backend.service.LessonService;
import com.minhdan.english_practice_backend.service.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {

    private final UserLessonRepository userLessonRepository;
    private final TelegramService telegramService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public List<LessonDto> syncLessons(User user, List<SyncLessonRequest> requests) {
        int newCount = 0;
        int updatedCount = 0;

        for (SyncLessonRequest req : requests) {
            Optional<UserLesson> existing = userLessonRepository.findByUserAndLessonId(user, req.getLessonId());

            if (existing.isPresent()) {
                // Update existing
                UserLesson lesson = existing.get();
                lesson.setTitle(req.getTitle());
                lesson.setPassage(req.getPassage());
                lesson.setPassageVi(req.getPassageVi());
                lesson.setImageBase64(req.getImageBase64());
                lesson.setImageUrl(req.getImageUrl());
                lesson.setWordsJson(toJson(req.getWords()));
                lesson.setSentencesJson(sentencesToJson(req.getSentences()));
                userLessonRepository.save(lesson);
                updatedCount++;
            } else {
                // Create new
                LocalDateTime createdAt = parseDateTime(req.getCreatedAt());
                UserLesson lesson = UserLesson.builder()
                        .user(user)
                        .lessonId(req.getLessonId())
                        .title(req.getTitle())
                        .passage(req.getPassage())
                        .passageVi(req.getPassageVi())
                        .imageBase64(req.getImageBase64())
                        .imageUrl(req.getImageUrl())
                        .wordsJson(toJson(req.getWords()))
                        .sentencesJson(sentencesToJson(req.getSentences()))
                        .createdAt(createdAt != null ? createdAt : LocalDateTime.now())
                        .build();
                userLessonRepository.save(lesson);
                newCount++;
            }
        }

        log.info("📖 Lesson sync: user={}, new={}, updated={}", user.getId(), newCount, updatedCount);
        telegramService.sendMessage("📖 <b>Lesson Sync</b>\n- User: " + user.getId()
                + "\n- New: " + newCount + ", Updated: " + updatedCount
                + "\n- Total synced: " + requests.size());

        return getUserLessons(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonDto> getUserLessons(User user) {
        List<UserLesson> lessons = userLessonRepository.findByUserOrderByCreatedAtDesc(user);
        return lessons.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public void deleteLesson(User user, String lessonId) {
        userLessonRepository.deleteByUserAndLessonId(user, lessonId);
        log.info("🗑️ Lesson deleted: user={}, lessonId={}", user.getId(), lessonId);
    }

    // ─── Helpers ─────────────────────────────────────────

    private LessonDto toDto(UserLesson entity) {
        return LessonDto.builder()
                .lessonId(entity.getLessonId())
                .title(entity.getTitle())
                .passage(entity.getPassage())
                .passageVi(entity.getPassageVi())
                .imageBase64(entity.getImageBase64())
                .imageUrl(entity.getImageUrl())
                .words(fromJson(entity.getWordsJson()))
                .sentences(sentencesFromJson(entity.getSentencesJson()))
                .createdAt(entity.getCreatedAt() != null
                        ? entity.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        : null)
                .build();
    }

    private String toJson(List<Map<String, Object>> words) {
        try {
            if (words == null) return "[]";
            return objectMapper.writeValueAsString(words);
        } catch (Exception e) {
            log.warn("Failed to serialize words: {}", e.getMessage());
            return "[]";
        }
    }

    private List<Map<String, Object>> fromJson(String json) {
        try {
            if (json == null || json.isEmpty()) return Collections.emptyList();
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to deserialize words: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String sentencesToJson(List<Map<String, String>> sentences) {
        try {
            if (sentences == null) return null;
            return objectMapper.writeValueAsString(sentences);
        } catch (Exception e) {
            log.warn("Failed to serialize sentences: {}", e.getMessage());
            return null;
        }
    }

    private List<Map<String, String>> sentencesFromJson(String json) {
        try {
            if (json == null || json.isEmpty()) return null;
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to deserialize sentences: {}", e.getMessage());
            return null;
        }
    }

    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception e2) {
                return null;
            }
        }
    }
}
