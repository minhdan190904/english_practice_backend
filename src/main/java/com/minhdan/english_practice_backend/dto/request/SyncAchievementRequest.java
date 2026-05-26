package com.minhdan.english_practice_backend.dto.request;

import lombok.Data;

@Data
public class SyncAchievementRequest {
    private String achievementId;
    private Integer currentProgress;
    private Boolean unlocked;
    private String unlockedAt;
}
