package com.minhdan.english_practice_backend.service;

import com.minhdan.english_practice_backend.dto.request.SyncAchievementRequest;
import com.minhdan.english_practice_backend.dto.response.AchievementDto;
import com.minhdan.english_practice_backend.entity.User;

import java.util.List;

public interface AchievementService {

    /**
     * Sync achievements from client. Upserts by achievementId, returns full list.
     */
    List<AchievementDto> syncAchievements(User user, List<SyncAchievementRequest> requests);

    /**
     * Get all achievements for user.
     */
    List<AchievementDto> getUserAchievements(User user);
}