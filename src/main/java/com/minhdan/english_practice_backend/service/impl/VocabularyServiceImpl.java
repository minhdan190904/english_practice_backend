package com.minhdan.english_practice_backend.service.impl;

import com.minhdan.english_practice_backend.dto.request.SyncVocabularyRequest;
import com.minhdan.english_practice_backend.dto.response.UserVocabularyDto;
import com.minhdan.english_practice_backend.entity.User;
import com.minhdan.english_practice_backend.entity.UserVocabulary;
import com.minhdan.english_practice_backend.repository.UserVocabularyRepository;
import com.minhdan.english_practice_backend.service.VocabularyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VocabularyServiceImpl implements VocabularyService {

    private final UserVocabularyRepository userVocabularyRepository;

    @Override
    @Transactional
    public List<UserVocabularyDto> syncUserVocabularies(User user, List<SyncVocabularyRequest> requests) {
        List<UserVocabulary> syncedVocabularies = requests.stream().map(request -> {
            Optional<UserVocabulary> existingOpt = userVocabularyRepository.findByUserAndWord(user, request.getWord());

            UserVocabulary vocab = existingOpt.orElseGet(() -> UserVocabulary.builder()
                    .user(user)
                    .word(request.getWord())
                    .build());

            // Upsert fields
            vocab.setStatus(request.getStatus());
            vocab.setUserDefinition(request.getUserDefinition());
            vocab.setWordDataJson(request.getWordDataJson());
            vocab.setNextReviewDate(request.getNextReviewDate());

            return userVocabularyRepository.save(vocab);
        }).collect(Collectors.toList());

        return syncedVocabularies.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    private UserVocabularyDto mapToDto(UserVocabulary entity) {
        return UserVocabularyDto.builder()
                .id(entity.getId())
                .word(entity.getWord())
                .status(entity.getStatus())
                .userDefinition(entity.getUserDefinition())
                .wordDataJson(entity.getWordDataJson())
                .nextReviewDate(entity.getNextReviewDate())
                .build();
    }
}
