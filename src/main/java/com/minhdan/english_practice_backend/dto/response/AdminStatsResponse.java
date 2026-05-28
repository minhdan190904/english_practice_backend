package com.minhdan.english_practice_backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminStatsResponse {
    private long totalUsers;
    private long activeUsersToday;
    private long anonymousUsers;
    private long linkedUsers;
    private long totalGrammarQuizCompleted;
}
