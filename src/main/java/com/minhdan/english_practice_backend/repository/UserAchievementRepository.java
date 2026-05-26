package com.minhdan.english_practice_backend.repository;

import com.minhdan.english_practice_backend.entity.User;
import com.minhdan.english_practice_backend.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    List<UserAchievement> findByUser(User user);

    Optional<UserAchievement> findByUserAndAchievementId(User user, String achievementId);
}
