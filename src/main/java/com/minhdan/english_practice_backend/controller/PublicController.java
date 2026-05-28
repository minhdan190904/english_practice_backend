package com.minhdan.english_practice_backend.controller;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.minhdan.english_practice_backend.entity.User;
import com.minhdan.english_practice_backend.repository.*;
import com.minhdan.english_practice_backend.service.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicController {

    private final UserRepository userRepository;
    private final UserGrammarQuizRepository userGrammarQuizRepository;
    private final UserGrammarMarkRepository userGrammarMarkRepository;
    private final UserSettingRepository userSettingRepository;
    private final UserStreakRepository userStreakRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final StudySessionRepository studySessionRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserLessonRepository userLessonRepository;
    private final UserVocabularyRepository userVocabularyRepository;
    private final TelegramService telegramService;

    /**
     * Step 1: Verify Google login and return user info.
     * User sends their Firebase ID token from Google Sign-In on web.
     */
    @PostMapping("/delete-account/verify")
    public ResponseEntity<?> verifyForDeletion(@RequestBody Map<String, String> request) {
        String idToken = request.get("idToken");

        if (idToken == null || idToken.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Google ID token is required"));
        }

        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String email = decoded.getEmail();

            if (email == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No email associated with this Google account"));
            }

            Optional<User> userOpt = userRepository.findByEmail(email);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No VG English account found linked to this Google account (" + email + "). Only Google-linked accounts can be deleted through this page."));
            }

            User user = userOpt.get();

            if (user.getIsAnonymous()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "This account is not linked to Google. Only Google-linked accounts can be deleted."));
            }

            return ResponseEntity.ok(Map.of(
                    "userId", user.getId(),
                    "email", user.getEmail(),
                    "displayName", user.getDisplayName() != null ? user.getDisplayName() : "",
                    "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                    "createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : ""
            ));

        } catch (Exception e) {
            log.error("❌ Failed to verify Google token for deletion: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid Google authentication. Please try signing in again."));
        }
    }

    /**
     * Step 2: Confirm and execute account deletion.
     * User must send their Firebase ID token again for security.
     */
    @PostMapping("/delete-account/confirm")
    @Transactional
    public ResponseEntity<?> confirmAccountDeletion(@RequestBody Map<String, String> request) {
        String idToken = request.get("idToken");

        if (idToken == null || idToken.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Google ID token is required"));
        }

        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String email = decoded.getEmail();

            if (email == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No email associated with this Google account"));
            }

            Optional<User> userOpt = userRepository.findByEmail(email);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Account not found"));
            }

            User user = userOpt.get();

            if (user.getIsAnonymous()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Cannot delete anonymous accounts via web"));
            }

            // Store info before deletion for Telegram notification
            String userId = user.getId();
            String userEmail = user.getEmail();
            String userName = user.getDisplayName();
            String deviceId = user.getDeviceId();
            String firebaseUid = user.getFirebaseUid();

            // Delete all child records
            refreshTokenRepository.deleteByUser(user);
            studySessionRepository.deleteByUser(user);
            userAchievementRepository.deleteByUser(user);
            userGrammarQuizRepository.deleteByUser(user);
            userGrammarMarkRepository.deleteByUser(user);
            userLessonRepository.deleteByUser(user);
            userVocabularyRepository.deleteByUser(user);
            userSettingRepository.deleteById(user.getId());
            userStreakRepository.deleteById(user.getId());

            // Delete the user
            userRepository.delete(user);

            log.info("🗑️ Account deleted via web: id={}, email={}", userId, userEmail);

            // Send detailed Telegram notification
            telegramService.sendMessage("🗑️ <b>Account Deleted (Web)</b>\n"
                    + "━━━━━━━━━━━━━━━━━━━━━\n"
                    + "👤 <b>User ID:</b> <code>" + userId + "</code>\n"
                    + "📧 <b>Email:</b> " + userEmail + "\n"
                    + "🏷️ <b>Name:</b> " + (userName != null ? userName : "N/A") + "\n"
                    + "📱 <b>Device ID:</b> " + (deviceId != null ? deviceId : "N/A") + "\n"
                    + "🔥 <b>Firebase UID:</b> <code>" + (firebaseUid != null ? firebaseUid : "N/A") + "</code>\n"
                    + "━━━━━━━━━━━━━━━━━━━━━\n"
                    + "✅ All user data has been permanently deleted.");

            return ResponseEntity.ok(Map.of(
                    "message", "Your account and all associated data have been permanently deleted."
            ));

        } catch (Exception e) {
            log.error("❌ Failed to delete account: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete account. Please try again or contact support."));
        }
    }
}
