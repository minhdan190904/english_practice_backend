package com.minhdan.english_practice_backend.controller;

import com.minhdan.english_practice_backend.dto.request.AdminLoginRequest;
import com.minhdan.english_practice_backend.dto.response.AdminStatsResponse;
import com.minhdan.english_practice_backend.dto.response.AdminUserDto;
import com.minhdan.english_practice_backend.entity.User;
import com.minhdan.english_practice_backend.entity.enums.Role;
import com.minhdan.english_practice_backend.repository.*;
import com.minhdan.english_practice_backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

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
    private final JwtService jwtService;

    // ─── Public: Admin Login ─────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AdminLoginRequest request) {
        User user = userRepository.findById(request.getUsername()).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }

        if (user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied. Admin role required."));
        }

        if (user.getPassword() == null || !user.getPassword().equals(request.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }

        String accessToken = jwtService.generateAccessToken(user);
        log.info("🔐 Admin login: id={}, email={}", user.getId(), user.getEmail());

        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", accessToken);
        response.put("user", AdminUserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .build());

        return ResponseEntity.ok(response);
    }

    // ─── Protected: Dashboard Stats ──────────────────────

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@AuthenticationPrincipal User currentUser) {
        if (currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin access required"));
        }

        long totalUsers = userRepository.count();
        long anonymousUsers = userRepository.countByIsAnonymousTrue();
        long linkedUsers = userRepository.countByIsAnonymousFalse();
        long activeToday = userRepository.countByUpdatedAtAfter(
                LocalDateTime.now().minusHours(24));
        long totalQuizCompleted = userGrammarQuizRepository.count();

        return ResponseEntity.ok(AdminStatsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsersToday(activeToday)
                .anonymousUsers(anonymousUsers)
                .linkedUsers(linkedUsers)
                .totalGrammarQuizCompleted(totalQuizCompleted)
                .build());
    }

    // ─── Protected: List All Users ───────────────────────

    @GetMapping("/users")
    public ResponseEntity<?> listUsers(@AuthenticationPrincipal User currentUser) {
        if (currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin access required"));
        }

        List<AdminUserDto> users = userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(u -> AdminUserDto.builder()
                        .id(u.getId())
                        .email(u.getEmail())
                        .displayName(u.getDisplayName())
                        .avatarUrl(u.getAvatarUrl())
                        .role(u.getRole().name())
                        .isAnonymous(u.getIsAnonymous())
                        .deviceId(u.getDeviceId())
                        .createdAt(u.getCreatedAt())
                        .updatedAt(u.getUpdatedAt())
                        .build())
                .toList();

        return ResponseEntity.ok(users);
    }

    // ─── Protected: Delete User ──────────────────────────

    @DeleteMapping("/users/{userId}")
    @Transactional
    public ResponseEntity<?> deleteUser(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String userId) {
        if (currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin access required"));
        }

        if (currentUser.getId().equals(userId)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Cannot delete your own admin account"));
        }

        User targetUser = userRepository.findById(userId).orElse(null);
        if (targetUser == null) {
            return ResponseEntity.notFound().build();
        }

        // Delete all child records first to avoid FK constraint violations
        refreshTokenRepository.deleteByUser(targetUser);
        studySessionRepository.deleteByUser(targetUser);
        userAchievementRepository.deleteByUser(targetUser);
        userGrammarQuizRepository.deleteByUser(targetUser);
        userGrammarMarkRepository.deleteByUser(targetUser);
        userLessonRepository.deleteByUser(targetUser);
        userVocabularyRepository.deleteByUser(targetUser);
        userSettingRepository.deleteById(targetUser.getId());
        userStreakRepository.deleteById(targetUser.getId());

        userRepository.delete(targetUser);
        log.info("🗑️ Admin deleted user: {} (email: {})", userId, targetUser.getEmail());

        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }
}
