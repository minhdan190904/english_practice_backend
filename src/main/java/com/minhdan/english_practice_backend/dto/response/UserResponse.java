package com.minhdan.english_practice_backend.dto.response;

import com.minhdan.english_practice_backend.entity.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String displayName;
    private String avatarUrl;
    private Role role;
    private Integer dailyGoal;
    private Boolean isNotificationEnabled;
    private Integer currentStreak;
    private Integer maxStreak;
}
