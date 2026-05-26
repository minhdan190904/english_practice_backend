package com.minhdan.english_practice_backend.service;

import com.minhdan.english_practice_backend.entity.User;
import com.minhdan.english_practice_backend.entity.UserStreak;

public interface StreakService {

    /**
     * Get current streak for user. Creates default if not exists.
     */
    UserStreak getStreak(User user);

    /**
     * Check-in for today. Increments streak if not already checked in.
     */
    UserStreak checkIn(User user);
}
