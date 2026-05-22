package com.minhdan.english_practice_backend.service.impl;

import com.minhdan.english_practice_backend.dto.ProgressLogRequest;
import com.minhdan.english_practice_backend.dto.ProgressSummaryResponse;
import com.minhdan.english_practice_backend.entity.StudySession;
import com.minhdan.english_practice_backend.entity.User;
import com.minhdan.english_practice_backend.repository.StudySessionRepository;
import com.minhdan.english_practice_backend.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProgressServiceImpl implements ProgressService {

    private final StudySessionRepository studySessionRepository;

    @Override
    @Transactional
    public void logSession(User user, ProgressLogRequest request) {
        LocalDate today = LocalDate.now();
        Optional<StudySession> existingSessionOpt = studySessionRepository.findByUserAndSessionDate(user, today);

        if (existingSessionOpt.isPresent()) {
            StudySession session = existingSessionOpt.get();
            session.setTimeSpentSeconds(session.getTimeSpentSeconds() + request.getTimeSpentSeconds());
            session.setWordsLearned(session.getWordsLearned() + request.getWordsLearned());
            session.setLessonsCompleted(session.getLessonsCompleted() + request.getLessonsCompleted());
            studySessionRepository.save(session);
        } else {
            StudySession session = StudySession.builder()
                    .user(user)
                    .sessionDate(today)
                    .timeSpentSeconds(request.getTimeSpentSeconds())
                    .wordsLearned(request.getWordsLearned())
                    .lessonsCompleted(request.getLessonsCompleted())
                    .build();
            studySessionRepository.save(session);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ProgressSummaryResponse getProgressSummary(User user) {
        Integer totalTime = studySessionRepository.sumTimeSpentByUser(user);
        Integer totalWords = studySessionRepository.sumWordsLearnedByUser(user);
        Integer totalLessons = studySessionRepository.sumLessonsCompletedByUser(user);

        return ProgressSummaryResponse.builder()
                .totalTimeSpentSeconds(totalTime != null ? totalTime : 0)
                .totalWordsLearned(totalWords != null ? totalWords : 0)
                .totalLessonsCompleted(totalLessons != null ? totalLessons : 0)
                .build();
    }
}
