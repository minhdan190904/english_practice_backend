package com.minhdan.english_practice_backend.service.impl;

import com.google.firebase.auth.FirebaseToken;
import com.minhdan.english_practice_backend.dto.response.UserResponse;
import com.minhdan.english_practice_backend.entity.User;
import com.minhdan.english_practice_backend.entity.UserSetting;
import com.minhdan.english_practice_backend.entity.UserStreak;
import com.minhdan.english_practice_backend.entity.enums.Role;
import com.minhdan.english_practice_backend.repository.UserRepository;
import com.minhdan.english_practice_backend.repository.UserSettingRepository;
import com.minhdan.english_practice_backend.repository.UserStreakRepository;
import com.minhdan.english_practice_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserSettingRepository userSettingRepository;
    private final UserStreakRepository userStreakRepository;

    @Override
    @Transactional
    public User getOrCreateUserFromToken(FirebaseToken token) {
        String firebaseUid = token.getUid();
        return userRepository.findByFirebaseUid(firebaseUid)
                .orElseGet(() -> createNewUser(token));
    }

    private User createNewUser(FirebaseToken token) {
        User newUser = User.builder()
                .firebaseUid(token.getUid())
                .email(token.getEmail())
                .displayName(token.getName())
                .avatarUrl(token.getPicture())
                .role(Role.USER)
                .build();
        
        newUser = userRepository.save(newUser);

        UserSetting setting = UserSetting.builder()
                .user(newUser)
                .dailyGoal(10) // default 10 words
                .isNotificationEnabled(true)
                .build();
        userSettingRepository.save(setting);

        UserStreak streak = UserStreak.builder()
                .user(newUser)
                .currentStreak(0)
                .maxStreak(0)
                .lastActivityDate(LocalDate.now())
                .build();
        userStreakRepository.save(streak);

        return newUser;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserResponse(User currentUser) {
        Optional<UserSetting> settingOpt = userSettingRepository.findById(currentUser.getId());
        Optional<UserStreak> streakOpt = userStreakRepository.findById(currentUser.getId());

        UserSetting setting = settingOpt.orElse(new UserSetting());
        UserStreak streak = streakOpt.orElse(new UserStreak());

        return UserResponse.builder()
                .id(currentUser.getId())
                .email(currentUser.getEmail())
                .displayName(currentUser.getDisplayName())
                .avatarUrl(currentUser.getAvatarUrl())
                .role(currentUser.getRole())
                .dailyGoal(setting.getDailyGoal())
                .isNotificationEnabled(setting.getIsNotificationEnabled())
                .currentStreak(streak.getCurrentStreak())
                .maxStreak(streak.getMaxStreak())
                .build();
    }
}
