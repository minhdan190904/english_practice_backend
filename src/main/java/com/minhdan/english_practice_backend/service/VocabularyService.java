package com.minhdan.english_practice_backend.service;

import com.minhdan.english_practice_backend.dto.request.SyncVocabularyRequest;
import com.minhdan.english_practice_backend.dto.response.UserVocabularyDto;
import com.minhdan.english_practice_backend.entity.User;

import java.util.List;

public interface VocabularyService {
    List<UserVocabularyDto> syncUserVocabularies(User user, List<SyncVocabularyRequest> requests);
}
