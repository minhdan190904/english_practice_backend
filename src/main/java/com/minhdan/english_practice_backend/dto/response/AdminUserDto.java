package com.minhdan.english_practice_backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminUserDto {
    private String id;
    private String email;
    private String displayName;
    private String avatarUrl;
    private String role;
    private Boolean isAnonymous;
    private String deviceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
