package com.minhdan.english_practice_backend.repository;

import com.minhdan.english_practice_backend.entity.User;
import com.minhdan.english_practice_backend.entity.UserGrammarMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserGrammarMarkRepository extends JpaRepository<UserGrammarMark, Long> {
    List<UserGrammarMark> findByUser(User user);
    Optional<UserGrammarMark> findByUserAndLessonId(User user, Integer lessonId);
    void deleteByUser(User user);
}
