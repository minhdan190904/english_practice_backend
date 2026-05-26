package com.minhdan.english_practice_backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AchievementDto {
    private String achievementId;
    private Integer currentProgress;
    private Boolean unlocked;
    private String unlockedAt;
}
