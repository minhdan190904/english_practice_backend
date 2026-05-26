package com.minhdan.english_practice_backend.service.impl;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.minhdan.english_practice_backend.dto.response.AuthResponse;
import com.minhdan.english_practice_backend.dto.response.CheckGoogleResponse;
import com.minhdan.english_practice_backend.dto.response.UserResponse;
import com.minhdan.english_practice_backend.entity.RefreshToken;
import com.minhdan.english_practice_backend.entity.User;
import com.minhdan.english_practice_backend.entity.UserSetting;
import com.minhdan.english_practice_backend.entity.UserStreak;
import com.minhdan.english_practice_backend.entity.enums.Role;
import com.minhdan.english_practice_backend.repository.RefreshTokenRepository;
import com.minhdan.english_practice_backend.repository.UserRepository;
import com.minhdan.english_practice_backend.repository.UserSettingRepository;
import com.minhdan.english_practice_backend.repository.UserStreakRepository;
import com.minhdan.english_practice_backend.security.JwtService;
import com.minhdan.english_practice_backend.service.TelegramService;
import com.minhdan.english_practice_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserSettingRepository userSettingRepository;
    private final UserStreakRepository userStreakRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final TelegramService telegramService;

    // ─── Device ID Registration (NEW) ─────────────────────────────

    @Override
    @Transactional
    public AuthResponse registerWithDeviceId(String deviceId) {
        try {
            log.info("🔐 Register: deviceId={}", deviceId);

            // Find or create user by device ID
            User user = userRepository.findByDeviceId(deviceId)
                    .orElseGet(() -> createNewDeviceUser(deviceId));

            // Generate app JWT pair
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            // Store refresh token hash
            storeRefreshToken(user, refreshToken);

            // Build response
            UserResponse userResponse = getCurrentUserResponse(user);

            telegramService.sendMessage("✅ <b>Device Login</b>\n"
                    + "- ID: " + user.getId() + "\n"
                    + "- Device: " + deviceId + "\n"
                    + "- Email: " + (user.getEmail() != null ? user.getEmail() : "(chưa link)"));

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .user(userResponse)
                    .build();

        } catch (Exception e) {
            log.error("❌ Device register failed: {}", e.getMessage());
            throw new RuntimeException("Failed to register with device ID: " + e.getMessage(), e);
        }
    }

    // ─── Link Google (NEW) ────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse linkGoogle(User currentUser, String googleIdToken) {
        try {
            // Verify Google ID token via Firebase Admin SDK
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(googleIdToken);
            log.info("🔗 Link Google: userId={}, email={}", currentUser.getId(), decoded.getEmail());

            // Update user profile with Google info
            if (decoded.getEmail() != null) {
                currentUser.setEmail(decoded.getEmail());
            }
            if (decoded.getName() != null) {
                currentUser.setDisplayName(decoded.getName());
            }
            if (decoded.getPicture() != null) {
                currentUser.setAvatarUrl(decoded.getPicture());
            }
            currentUser.setIsAnonymous(false);
            // Store Firebase UID for reference
            currentUser.setFirebaseUid(decoded.getUid());

            currentUser = userRepository.save(currentUser);

            // Generate new JWT pair with updated info
            String accessToken = jwtService.generateAccessToken(currentUser);
            String refreshToken = jwtService.generateRefreshToken(currentUser);
            storeRefreshToken(currentUser, refreshToken);

            UserResponse userResponse = getCurrentUserResponse(currentUser);

            telegramService.sendMessage("🔗 <b>Google Linked</b>\n"
                    + "- ID: " + currentUser.getId() + "\n"
                    + "- Device: " + (currentUser.getDeviceId() != null ? currentUser.getDeviceId() : "N/A") + "\n"
                    + "- Email: " + currentUser.getEmail() + "\n"
                    + "- Name: " + currentUser.getDisplayName());

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .user(userResponse)
                    .build();

        } catch (Exception e) {
            log.error("❌ Link Google failed: {}", e.getMessage());
            throw new RuntimeException("Failed to link Google: " + e.getMessage(), e);
        }
    }

    // ─── Unlink Google (NEW) ──────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse unlinkGoogle(User currentUser) {
        try {
            log.info("🔓 Unlink Google: userId={}", currentUser.getId());

            String oldEmail = currentUser.getEmail();

            // Clear Google info
            currentUser.setEmail(null);
            currentUser.setDisplayName(null);
            currentUser.setAvatarUrl(null);
            currentUser.setFirebaseUid(null);
            currentUser.setIsAnonymous(true);

            currentUser = userRepository.save(currentUser);

            // Generate new JWT pair
            String accessToken = jwtService.generateAccessToken(currentUser);
            String refreshToken = jwtService.generateRefreshToken(currentUser);
            storeRefreshToken(currentUser, refreshToken);

            UserResponse userResponse = getCurrentUserResponse(currentUser);

            telegramService.sendMessage("🔓 <b>Google Unlinked</b>\n"
                    + "- ID: " + currentUser.getId() + "\n"
                    + "- Device: " + (currentUser.getDeviceId() != null ? currentUser.getDeviceId() : "N/A") + "\n"
                    + "- Old Email: " + (oldEmail != null ? oldEmail : "N/A"));

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .user(userResponse)
                    .build();

        } catch (Exception e) {
            log.error("❌ Unlink Google failed: {}", e.getMessage());
            throw new RuntimeException("Failed to unlink Google: " + e.getMessage(), e);
        }
    }

    // ─── Check Google ────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CheckGoogleResponse checkGoogle(String googleIdToken) {
        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(googleIdToken);
            String email = decoded.getEmail();
            log.info("🔍 Check Google: email={}", email);

            Optional<User> existingUser = email != null ? userRepository.findByEmail(email) : Optional.empty();

            if (existingUser.isPresent()) {
                User user = existingUser.get();
                return CheckGoogleResponse.builder()
                        .exists(true)
                        .existingUserId(user.getId())
                        .existingEmail(user.getEmail())
                        .existingDisplayName(user.getDisplayName())
                        .existingAvatarUrl(user.getAvatarUrl())
                        .build();
            }

            return CheckGoogleResponse.builder().exists(false).build();
        } catch (Exception e) {
            log.error("❌ Check Google failed: {}", e.getMessage());
            throw new RuntimeException("Failed to check Google: " + e.getMessage(), e);
        }
    }

    // ─── Switch to existing Google account ───────────────────────

    @Override
    @Transactional
    public AuthResponse switchToGoogleAccount(User currentUser, String googleIdToken) {
        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(googleIdToken);
            String email = decoded.getEmail();
            log.info("🔄 Switch to Google account: from userId={} to email={}", currentUser.getId(), email);

            User googleUser = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Google account not found"));

            // Generate new JWT pair for the Google user
            String accessToken = jwtService.generateAccessToken(googleUser);
            String refreshToken = jwtService.generateRefreshToken(googleUser);
            storeRefreshToken(googleUser, refreshToken);

            UserResponse userResponse = getCurrentUserResponse(googleUser);

            telegramService.sendMessage("🔄 <b>Account Switch</b>\n"
                    + "- From ID: " + currentUser.getId() + "\n"
                    + "- To ID: " + googleUser.getId() + "\n"
                    + "- Email: " + googleUser.getEmail());

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .user(userResponse)
                    .build();
        } catch (Exception e) {
            log.error("❌ Switch to Google failed: {}", e.getMessage());
            throw new RuntimeException("Failed to switch to Google account: " + e.getMessage(), e);
        }
    }

    // ─── Create new account with Google ──────────────────────────

    @Override
    @Transactional
    public AuthResponse createWithGoogle(User currentUser, String googleIdToken) {
        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(googleIdToken);
            log.info("🆕 Create with Google: email={}", decoded.getEmail());

            // Create new user with Google info
            User newUser = User.builder()
                    .firebaseUid(decoded.getUid())
                    .email(decoded.getEmail())
                    .displayName(decoded.getName())
                    .avatarUrl(decoded.getPicture())
                    .role(Role.USER)
                    .isAnonymous(false)
                    .build();
            newUser = userRepository.save(newUser);

            // Create settings and streak
            UserSetting setting = UserSetting.builder()
                    .user(newUser)
                    .dailyGoal(10)
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

            // Generate JWT pair for new user
            String accessToken = jwtService.generateAccessToken(newUser);
            String refreshToken = jwtService.generateRefreshToken(newUser);
            storeRefreshToken(newUser, refreshToken);

            UserResponse userResponse = getCurrentUserResponse(newUser);

            telegramService.sendMessage("🆕 <b>New Google Account</b>\n"
                    + "- New ID: " + newUser.getId() + "\n"
                    + "- Old ID: " + currentUser.getId() + "\n"
                    + "- Email: " + newUser.getEmail() + "\n"
                    + "- Name: " + newUser.getDisplayName());

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .user(userResponse)
                    .build();
        } catch (Exception e) {
            log.error("❌ Create with Google failed: {}", e.getMessage());
            throw new RuntimeException("Failed to create with Google: " + e.getMessage(), e);
        }
    }

    // ─── Legacy Firebase Token Registration ───────────────────────

    @Override
    @Transactional
    public User getOrCreateUserFromToken(FirebaseToken token) {
        String firebaseUid = token.getUid();
        return userRepository.findByFirebaseUid(firebaseUid)
                .map(existingUser -> updateUserIfNeeded(existingUser, token))
                .orElseGet(() -> createNewUser(token));
    }

    @Override
    @Transactional
    public AuthResponse registerWithFirebaseToken(String firebaseIdToken) {
        try {
            // 1. Verify Firebase token (one-time call to Firebase)
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(firebaseIdToken);
            log.info("🔐 Register: Firebase UID={}, email={}", decoded.getUid(), decoded.getEmail());

            // 2. Upsert user
            User user = getOrCreateUserFromToken(decoded);

            // 3. Generate app JWT pair
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            // 4. Store refresh token hash
            storeRefreshToken(user, refreshToken);

            // 5. Build response
            UserResponse userResponse = getCurrentUserResponse(user);
            
            telegramService.sendMessage("✅ <b>User Login/Register</b>\n- ID: " + user.getId() + "\n- Email: " + (user.getEmail() != null ? user.getEmail() : "Anonymous") + "\n- Name: " + (user.getDisplayName() != null ? user.getDisplayName() : "Unknown"));
            
            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .user(userResponse)
                    .build();

        } catch (Exception e) {
            log.error("❌ Register failed: {}", e.getMessage());
            throw new RuntimeException("Failed to register: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String rawRefreshToken) {
        try {
            // 1. Verify the refresh token JWT signature
            if (!jwtService.isTokenValid(rawRefreshToken)) {
                throw new RuntimeException("Invalid or expired refresh token");
            }

            // 2. Check token type
            String type = jwtService.extractTokenType(rawRefreshToken);
            if (!"REFRESH".equals(type)) {
                throw new RuntimeException("Token is not a refresh token");
            }

            // 3. Find the hashed token in DB
            String tokenHash = hashToken(rawRefreshToken);
            RefreshToken storedToken = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
                    .orElseThrow(() -> new RuntimeException("Refresh token not found or revoked"));

            // 4. Revoke old token
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);

            // 5. Get user
            Long userId = jwtService.extractUserId(rawRefreshToken);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 6. Generate new token pair
            String newAccessToken = jwtService.generateAccessToken(user);
            String newRefreshToken = jwtService.generateRefreshToken(user);
            storeRefreshToken(user, newRefreshToken);

            telegramService.sendMessage("🔄 <b>Token Refresh</b>\n- ID: " + user.getId() + "\n- Email: " + (user.getEmail() != null ? user.getEmail() : "Anonymous"));

            return AuthResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .build();

        } catch (Exception e) {
            log.error("❌ Refresh failed: {}", e.getMessage());
            throw new RuntimeException("Failed to refresh token: " + e.getMessage(), e);
        }
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

    // ─── Private helpers ─────────────────────────────────────────

    private User createNewDeviceUser(String deviceId) {
        User newUser = User.builder()
                .deviceId(deviceId)
                .firebaseUid("device_" + deviceId) // placeholder for non-null constraint compat
                .role(Role.USER)
                .isAnonymous(true)
                .build();

        newUser = userRepository.save(newUser);

        UserSetting setting = UserSetting.builder()
                .user(newUser)
                .dailyGoal(10)
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

        log.info("👤 Created new device user: id={}, deviceId={}", newUser.getId(), deviceId);
        return newUser;
    }

    private void storeRefreshToken(User user, String rawToken) {
        String hash = hashToken(rawToken);
        long expirationMs = jwtService.getRefreshTokenExpirationMs();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expirationMs / 1000);

        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(hash)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();
        refreshTokenRepository.save(entity);
    }

    private String hashToken(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

    private User updateUserIfNeeded(User user, FirebaseToken token) {
        boolean updated = false;

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
                .dailyGoal(10)
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
}
