package com.minhdan.english_practice_backend.service;

import com.minhdan.english_practice_backend.dto.ProgressLogRequest;
import com.minhdan.english_practice_backend.dto.ProgressSummaryResponse;
import com.minhdan.english_practice_backend.entity.User;

public interface ProgressService {
    void logSession(User user, ProgressLogRequest request);
    ProgressSummaryResponse getProgressSummary(User user);
}
