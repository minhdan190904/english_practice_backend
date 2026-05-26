package com.minhdan.english_practice_backend.service.impl;

import com.minhdan.english_practice_backend.dto.request.SyncAchievementRequest;
import com.minhdan.english_practice_backend.dto.response.AchievementDto;
import com.minhdan.english_practice_backend.entity.User;
import com.minhdan.english_practice_backend.entity.UserAchievement;
import com.minhdan.english_practice_backend.repository.UserAchievementRepository;
import com.minhdan.english_practice_backend.service.AchievementService;
import com.minhdan.english_practice_backend.service.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementServiceImpl implements AchievementService {

    private final UserAchievementRepository userAchievementRepository;
    private final TelegramService telegramService;

    @Override
    @Transactional
    public List<AchievementDto> syncAchievements(User user, List<SyncAchievementRequest> requests) {
        int newCount = 0;
        int updatedCount = 0;

        for (SyncAchievementRequest req : requests) {
            Optional<UserAchievement> existing = userAchievementRepository.findByUserAndAchievementId(user, req.getAchievementId());

            if (existing.isPresent()) {
                // Update existing — only if incoming progress is >= existing (don't downgrade)
                UserAchievement achievement = existing.get();
                if (req.getCurrentProgress() != null && req.getCurrentProgress() >= achievement.getCurrentProgress()) {
                    achievement.setCurrentProgress(req.getCurrentProgress());
                }
                if (req.getUnlocked() != null && req.getUnlocked()) {
                    achievement.setUnlocked(true);
                    if (req.getUnlockedAt() != null) {
                        achievement.setUnlockedAt(parseDateTime(req.getUnlockedAt()));
                    }
                }
                userAchievementRepository.save(achievement);
                updatedCount++;
            } else {
                // Create new
                UserAchievement achievement = UserAchievement.builder()
                        .user(user)
                        .achievementId(req.getAchievementId())
                        .currentProgress(req.getCurrentProgress() != null ? req.getCurrentProgress() : 0)
                        .unlocked(req.getUnlocked() != null ? req.getUnlocked() : false)
                        .unlockedAt(req.getUnlockedAt() != null ? parseDateTime(req.getUnlockedAt()) : null)
                        .build();
                userAchievementRepository.save(achievement);
                newCount++;
            }
        }

        log.info("🏆 Achievement sync: user={}, new={}, updated={}", user.getId(), newCount, updatedCount);
        telegramService.sendMessage("🏆 <b>Achievement Sync</b>\n- User: " + user.getId()
                + "\n- New: " + newCount + ", Updated: " + updatedCount
                + "\n- Total synced: " + requests.size());

        return getUserAchievements(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AchievementDto> getUserAchievements(User user) {
        List<UserAchievement> achievements = userAchievementRepository.findByUser(user);
        return achievements.stream().map(this::toDto).toList();
    }

    // ─── Helpers ─────────────────────────────────────────

    private AchievementDto toDto(UserAchievement entity) {
        return AchievementDto.builder()
                .achievementId(entity.getAchievementId())
                .currentProgress(entity.getCurrentProgress())
                .unlocked(entity.getUnlocked())
                .unlockedAt(entity.getUnlockedAt() != null
                        ? entity.getUnlockedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        : null)
                .build();
    }

    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception e2) {
                return null;
            }
        }
    }
}
