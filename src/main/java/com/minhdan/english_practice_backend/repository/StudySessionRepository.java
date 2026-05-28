package com.minhdan.english_practice_backend.repository;

import com.minhdan.english_practice_backend.entity.StudySession;
import com.minhdan.english_practice_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    Optional<StudySession> findByUserAndSessionDate(User user, LocalDate sessionDate);

    @Query("SELECT SUM(s.timeSpentSeconds) FROM StudySession s WHERE s.user = :user")
    Integer sumTimeSpentByUser(@Param("user") User user);

    @Query("SELECT SUM(s.wordsLearned) FROM StudySession s WHERE s.user = :user")
    Integer sumWordsLearnedByUser(@Param("user") User user);

    @Query("SELECT SUM(s.lessonsCompleted) FROM StudySession s WHERE s.user = :user")
    Integer sumLessonsCompletedByUser(@Param("user") User user);

    void deleteByUser(User user);
}
