package com.minhdan.english_practice_backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minhdan.english_practice_backend.dto.response.GrammarQuizDto;
import com.minhdan.english_practice_backend.entity.User;
import com.minhdan.english_practice_backend.entity.UserGrammarMark;
import com.minhdan.english_practice_backend.entity.UserGrammarQuiz;
import com.minhdan.english_practice_backend.repository.UserGrammarMarkRepository;
import com.minhdan.english_practice_backend.repository.UserGrammarQuizRepository;
import com.minhdan.english_practice_backend.service.GrammarQuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrammarQuizServiceImpl implements GrammarQuizService {

    private final UserGrammarQuizRepository userGrammarQuizRepository;
    private final UserGrammarMarkRepository userGrammarMarkRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<Integer, GrammarQuizDto> quizCache = new ConcurrentHashMap<>();

    @Override
    public GrammarQuizDto getQuizByTopicId(Integer topicId) {
        return quizCache.computeIfAbsent(topicId, this::loadQuizFromJson);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> getCompletedTopics(User user) {
        return userGrammarQuizRepository.findByUser(user).stream()
                .filter(UserGrammarQuiz::getCompleted)
                .map(UserGrammarQuiz::getTopicId)
                .toList();
    }

    @Override
    @Transactional
    public void markTopicCompleted(User user, Integer topicId) {
        Optional<UserGrammarQuiz> existing = userGrammarQuizRepository.findByUserAndTopicId(user, topicId);

        if (existing.isPresent()) {
            UserGrammarQuiz quiz = existing.get();
            if (!quiz.getCompleted()) {
                quiz.setCompleted(true);
                userGrammarQuizRepository.save(quiz);
            }
        } else {
            UserGrammarQuiz quiz = UserGrammarQuiz.builder()
                    .user(user)
                    .topicId(topicId)
                    .completed(true)
                    .build();
            userGrammarQuizRepository.save(quiz);
        }

        log.info("✅ Grammar quiz completed: user={}, topicId={}", user.getId(), topicId);
    }

    @Override
    @Transactional
    public List<Integer> syncCompletedTopics(User user, List<Integer> completedTopicIds) {
        int newCount = 0;
        int updatedCount = 0;

        for (Integer topicId : completedTopicIds) {
            Optional<UserGrammarQuiz> existing = userGrammarQuizRepository.findByUserAndTopicId(user, topicId);

            if (existing.isPresent()) {
                UserGrammarQuiz quiz = existing.get();
                if (!quiz.getCompleted()) {
                    quiz.setCompleted(true);
                    userGrammarQuizRepository.save(quiz);
                    updatedCount++;
                }
            } else {
                UserGrammarQuiz quiz = UserGrammarQuiz.builder()
                        .user(user)
                        .topicId(topicId)
                        .completed(true)
                        .build();
                userGrammarQuizRepository.save(quiz);
                newCount++;
            }
        }

        log.info("📝 Grammar quiz sync: user={}, new={}, updated={}, total synced={}",
                user.getId(), newCount, updatedCount, completedTopicIds.size());

        // Return merged list of all completed topics
        return getCompletedTopics(user);
    }

    // ─── Helpers ─────────────────────────────────────────

    private GrammarQuizDto loadQuizFromJson(Integer topicId) {
        String path = "json/grammar_quizzes/topic_" + topicId + ".json";
        try {
            ClassPathResource resource = new ClassPathResource(path);
            try (InputStream is = resource.getInputStream()) {
                GrammarQuizDto quiz = objectMapper.readValue(is, GrammarQuizDto.class);
                log.info("📖 Loaded grammar quiz from classpath: {}", path);
                return quiz;
            }
        } catch (IOException e) {
            log.error("❌ Failed to load grammar quiz JSON: {}", path, e);
            throw new RuntimeException("Grammar quiz not found for topic: " + topicId, e);
        }
    }

    // ─── Lesson Marks ────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Boolean> getLessonMarks(User user) {
        Map<Integer, Boolean> result = new HashMap<>();
        for (UserGrammarMark mark : userGrammarMarkRepository.findByUser(user)) {
            result.put(mark.getLessonId(), mark.getMarked());
        }
        return result;
    }

    @Override
    @Transactional
    public Map<Integer, Boolean> syncLessonMarks(User user, Map<Integer, Boolean> marks) {
        int newCount = 0;
        int updatedCount = 0;

        for (Map.Entry<Integer, Boolean> entry : marks.entrySet()) {
            Integer lessonId = entry.getKey();
            Boolean marked = entry.getValue();

            Optional<UserGrammarMark> existing = userGrammarMarkRepository.findByUserAndLessonId(user, lessonId);

            if (existing.isPresent()) {
                UserGrammarMark mark = existing.get();
                if (!mark.getMarked().equals(marked)) {
                    mark.setMarked(marked);
                    userGrammarMarkRepository.save(mark);
                    updatedCount++;
                }
            } else {
                UserGrammarMark mark = UserGrammarMark.builder()
                        .user(user)
                        .lessonId(lessonId)
                        .marked(marked)
                        .build();
                userGrammarMarkRepository.save(mark);
                newCount++;
            }
        }

        log.info("📋 Grammar marks sync: user={}, new={}, updated={}",
                user.getId(), newCount, updatedCount);

        return getLessonMarks(user);
    }
}
