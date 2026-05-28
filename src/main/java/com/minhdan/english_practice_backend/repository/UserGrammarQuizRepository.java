package com.minhdan.english_practice_backend.repository;

import com.minhdan.english_practice_backend.entity.User;
import com.minhdan.english_practice_backend.entity.UserGrammarQuiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserGrammarQuizRepository extends JpaRepository<UserGrammarQuiz, Long> {

    List<UserGrammarQuiz> findByUser(User user);

    Optional<UserGrammarQuiz> findByUserAndTopicId(User user, Integer topicId);

    void deleteByUser(User user);
}
