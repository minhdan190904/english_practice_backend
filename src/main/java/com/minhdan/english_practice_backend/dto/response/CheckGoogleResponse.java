package com.minhdan.english_practice_backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckGoogleResponse {
    private boolean exists;
    private Long existingUserId;
    private String existingEmail;
    private String existingDisplayName;
    private String existingAvatarUrl;
}
