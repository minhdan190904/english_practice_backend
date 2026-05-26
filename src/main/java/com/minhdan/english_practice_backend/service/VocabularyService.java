package com.minhdan.english_practice_backend.service;

import com.minhdan.english_practice_backend.dto.request.SyncVocabularyRequest;
import com.minhdan.english_practice_backend.dto.response.UserVocabularyDto;
import com.minhdan.english_practice_backend.entity.User;

import java.util.List;

public interface VocabularyService {
    List<UserVocabularyDto> syncUserVocabularies(User user, List<SyncVocabularyRequest> requests);

    /**
     * Get vocabularies due for SRS review (nextReviewDate <= now).
     */
    List<UserVocabularyDto> getDueForReview(User user);

    /**
     * Submit review result using SM-2 algorithm.
     * @param quality 0-5 (0=forgot, 5=perfect)
     */
    UserVocabularyDto submitReview(User user, String word, int quality);

    /**
     * Get all vocabularies for user.
     */
    List<UserVocabularyDto> getAllForUser(User user);
}
