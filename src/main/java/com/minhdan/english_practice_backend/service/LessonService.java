package com.minhdan.english_practice_backend.service;

import com.minhdan.english_practice_backend.dto.request.SyncLessonRequest;
import com.minhdan.english_practice_backend.dto.response.LessonDto;
import com.minhdan.english_practice_backend.entity.User;

import java.util.List;

public interface LessonService {

    /**
     * Sync lessons from client. Upserts by lessonId, returns full list.
     */
    List<LessonDto> syncLessons(User user, List<SyncLessonRequest> requests);

    /**
     * Get all lessons for user (newest first).
     */
    List<LessonDto> getUserLessons(User user);

    /**
     * Delete a specific lesson.
     */
    void deleteLesson(User user, String lessonId);
}
