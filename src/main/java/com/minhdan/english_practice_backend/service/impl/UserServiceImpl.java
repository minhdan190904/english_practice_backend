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
                .map(existingUser -> updateUserIfNeeded(existingUser, token))
                .orElseGet(() -> createNewUser(token));
    }

    private User updateUserIfNeeded(User user, FirebaseToken token) {
        boolean updated = false;

        // When user links Google account, token will now have email/name/picture
        if (token.getEmail() != null && !token.getEmail().equals(user.getEmail())) {
            user.setEmail(token.getEmail());
            updated = true;
        }
        if (token.getName() != null && !token.getName().equals(user.getDisplayName())) {
            user.setDisplayName(token.getName());
            updated = true;
        }
        if (token.getPicture() != null && !token.getPicture().equals(user.getAvatarUrl())) {
            user.setAvatarUrl(token.getPicture());
            updated = true;
        }

        // If user now has email, they are no longer anonymous
        if (token.getEmail() != null && user.getIsAnonymous()) {
            user.setIsAnonymous(false);
            updated = true;
        }

        if (updated) {
            user = userRepository.save(user);
        }
        return user;
    }

    private User createNewUser(FirebaseToken token) {
        boolean isAnonymous = token.getEmail() == null;
        User newUser = User.builder()
                .firebaseUid(token.getUid())
                .email(token.getEmail())
                .displayName(token.getName())
                .avatarUrl(token.getPicture())
                .role(Role.USER)
                .isAnonymous(isAnonymous)
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
                .isAnonymous(currentUser.getIsAnonymous())
                .dailyGoal(setting.getDailyGoal())
                .isNotificationEnabled(setting.getIsNotificationEnabled())
                .currentStreak(streak.getCurrentStreak())
                .maxStreak(streak.getMaxStreak())
                .build();
    }
}
