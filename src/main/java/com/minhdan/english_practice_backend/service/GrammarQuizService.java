package com.minhdan.english_practice_backend.service;

import com.minhdan.english_practice_backend.dto.response.GrammarQuizDto;
import com.minhdan.english_practice_backend.entity.User;

import java.util.List;
import java.util.Map;

public interface GrammarQuizService {

    /**
     * Load quiz questions for a specific topic from JSON.
     */
    GrammarQuizDto getQuizByTopicId(Integer topicId);

    /**
     * Get all completed topic IDs for a user.
     */
    List<Integer> getCompletedTopics(User user);

    /**
     * Mark a single topic as completed for a user.
     */
    void markTopicCompleted(User user, Integer topicId);

    /**
     * Sync completed topics from client. Upserts and returns merged list.
     */
    List<Integer> syncCompletedTopics(User user, List<Integer> completedTopicIds);

    /**
     * Get all lesson marks (read/studied) for a user.
     */
    Map<Integer, Boolean> getLessonMarks(User user);

    /**
     * Sync lesson marks from client. Upserts and returns merged map.
     */
    Map<Integer, Boolean> syncLessonMarks(User user, Map<Integer, Boolean> marks);
}
