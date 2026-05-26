package com.minhdan.english_practice_backend.service.impl;

import com.minhdan.english_practice_backend.entity.User;
import com.minhdan.english_practice_backend.entity.UserStreak;
import com.minhdan.english_practice_backend.repository.UserStreakRepository;
import com.minhdan.english_practice_backend.service.StreakService;
import com.minhdan.english_practice_backend.service.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreakServiceImpl implements StreakService {

    private final UserStreakRepository userStreakRepository;
    private final TelegramService telegramService;

    @Override
    @Transactional(readOnly = true)
    public UserStreak getStreak(User user) {
        return userStreakRepository.findById(user.getId())
                .orElseGet(() -> createDefaultStreak(user));
    }

    @Override
    @Transactional
    public UserStreak checkIn(User user) {
        UserStreak streak = userStreakRepository.findById(user.getId())
                .orElseGet(() -> createDefaultStreak(user));

        LocalDate today = LocalDate.now();
        LocalDate lastActivity = streak.getLastActivityDate();

        if (lastActivity != null && lastActivity.equals(today)) {
            // Already checked in today
            return streak;
        }

        if (lastActivity != null && lastActivity.equals(today.minusDays(1))) {
            // Consecutive day — increment streak
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);
        } else {
            // Streak broken — reset to 1
            streak.setCurrentStreak(1);
        }

        // Update max streak
        if (streak.getCurrentStreak() > streak.getMaxStreak()) {
            streak.setMaxStreak(streak.getCurrentStreak());
        }

        streak.setLastActivityDate(today);
        streak = userStreakRepository.save(streak);

        log.info("🔥 Streak check-in: user={}, current={}, max={}",
                user.getId(), streak.getCurrentStreak(), streak.getMaxStreak());

        if (streak.getCurrentStreak() >= 7 && streak.getCurrentStreak() % 7 == 0) {
            telegramService.sendMessage("🔥 <b>Streak Milestone!</b>\n- User: " + user.getId()
                    + "\n- Streak: " + streak.getCurrentStreak() + " days 🎉");
        }

        return streak;
    }

    private UserStreak createDefaultStreak(User user) {
        UserStreak streak = UserStreak.builder()
                .user(user)
                .currentStreak(0)
                .maxStreak(0)
                .lastActivityDate(LocalDate.now())
                .build();
        return userStreakRepository.save(streak);
    }
}
