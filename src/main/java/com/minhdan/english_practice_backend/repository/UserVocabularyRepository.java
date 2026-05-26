package com.minhdan.english_practice_backend.repository;

import com.minhdan.english_practice_backend.entity.User;
import com.minhdan.english_practice_backend.entity.UserVocabulary;
import com.minhdan.english_practice_backend.entity.enums.WordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserVocabularyRepository extends JpaRepository<UserVocabulary, Long> {
    List<UserVocabulary> findByUserAndStatus(User user, WordStatus status);
    Optional<UserVocabulary> findByUserAndWord(User user, String word);
    List<UserVocabulary> findByUserAndNextReviewDateBeforeOrderByNextReviewDateAsc(User user, LocalDateTime now);
    List<UserVocabulary> findByUser(User user);
}
