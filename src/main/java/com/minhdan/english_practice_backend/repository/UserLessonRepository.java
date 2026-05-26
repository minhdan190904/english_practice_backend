package com.minhdan.english_practice_backend.repository;

import com.minhdan.english_practice_backend.entity.User;
import com.minhdan.english_practice_backend.entity.UserLesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLessonRepository extends JpaRepository<UserLesson, Long> {

    List<UserLesson> findByUserOrderByCreatedAtDesc(User user);

    Optional<UserLesson> findByUserAndLessonId(User user, String lessonId);

    void deleteByUserAndLessonId(User user, String lessonId);

    long countByUser(User user);
}
