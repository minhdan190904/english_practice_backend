package com.minhdan.english_practice_backend.service.impl;

import com.minhdan.english_practice_backend.dto.request.SyncVocabularyRequest;
import com.minhdan.english_practice_backend.dto.response.UserVocabularyDto;
import com.minhdan.english_practice_backend.entity.User;
import com.minhdan.english_practice_backend.entity.UserVocabulary;
import com.minhdan.english_practice_backend.repository.UserVocabularyRepository;
import com.minhdan.english_practice_backend.service.VocabularyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
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

            // Upsert fields — normalize legacy statuses (STARRED/LEARNING → STUDYING)
            vocab.setStatus(request.getStatus() != null ? request.getStatus().normalize() : null);
            vocab.setUserDefinition(request.getUserDefinition());
            vocab.setWordDataJson(request.getWordDataJson());
            vocab.setNextReviewDate(request.getNextReviewDate());

            // SRS fields (null-safe)
            if (request.getEaseFactor() != null) vocab.setEaseFactor(request.getEaseFactor());
            if (request.getSrsInterval() != null) vocab.setSrsInterval(request.getSrsInterval());
            if (request.getRepetitions() != null) vocab.setRepetitions(request.getRepetitions());
            if (request.getLastReviewDate() != null) vocab.setLastReviewDate(request.getLastReviewDate());

            return userVocabularyRepository.save(vocab);
        }).collect(Collectors.toList());

        return syncedVocabularies.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserVocabularyDto> getDueForReview(User user) {
        List<UserVocabulary> dueWords = userVocabularyRepository
                .findByUserAndNextReviewDateBeforeOrderByNextReviewDateAsc(user, LocalDateTime.now());
        return dueWords.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserVocabularyDto submitReview(User user, String word, int quality) {
        UserVocabulary vocab = userVocabularyRepository.findByUserAndWord(user, word)
                .orElseThrow(() -> new RuntimeException("Vocabulary not found: " + word));

        // SM-2 Algorithm
        double ef = vocab.getEaseFactor() != null ? vocab.getEaseFactor() : 2.5;
        int reps = vocab.getRepetitions() != null ? vocab.getRepetitions() : 0;
        int interval = vocab.getSrsInterval() != null ? vocab.getSrsInterval() : 0;

        if (quality >= 3) {
            // Correct response
            if (reps == 0) {
                interval = 1;
            } else if (reps == 1) {
                interval = 6;
            } else {
                interval = (int) Math.round(interval * ef);
            }
            reps++;
        } else {
            // Incorrect — reset
            reps = 0;
            interval = 1;
        }

        // Update ease factor: EF' = EF + (0.1 - (5-q)*(0.08 + (5-q)*0.02))
        ef = ef + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
        if (ef < 1.3) ef = 1.3;

        vocab.setEaseFactor(ef);
        vocab.setRepetitions(reps);
        vocab.setSrsInterval(interval);
        vocab.setLastReviewDate(LocalDateTime.now());
        vocab.setNextReviewDate(LocalDateTime.now().plusDays(interval));

        vocab = userVocabularyRepository.save(vocab);
        log.info("🧠 SRS review: user={}, word={}, quality={}, nextReview={}",
                user.getId(), word, quality, vocab.getNextReviewDate());

        return mapToDto(vocab);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserVocabularyDto> getAllForUser(User user) {
        return userVocabularyRepository.findByUser(user).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private UserVocabularyDto mapToDto(UserVocabulary entity) {
        return UserVocabularyDto.builder()
                .id(entity.getId())
                .word(entity.getWord())
                .status(entity.getStatus() != null ? entity.getStatus().normalize() : null)
                .userDefinition(entity.getUserDefinition())
                .wordDataJson(entity.getWordDataJson())
                .nextReviewDate(entity.getNextReviewDate())
                .easeFactor(entity.getEaseFactor())
                .srsInterval(entity.getSrsInterval())
                .repetitions(entity.getRepetitions())
                .lastReviewDate(entity.getLastReviewDate())
                .build();
    }
}
