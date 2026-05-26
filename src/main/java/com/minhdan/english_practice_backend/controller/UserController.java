package com.minhdan.english_practice_backend.controller;

import com.minhdan.english_practice_backend.dto.request.LinkGoogleRequest;
import com.minhdan.english_practice_backend.dto.request.RefreshTokenRequest;
import com.minhdan.english_practice_backend.dto.request.RegisterDeviceRequest;
import com.minhdan.english_practice_backend.dto.request.RegisterRequest;
import com.minhdan.english_practice_backend.dto.response.AuthResponse;
import com.minhdan.english_practice_backend.dto.response.CheckGoogleResponse;
import com.minhdan.english_practice_backend.dto.response.UserResponse;
import com.minhdan.english_practice_backend.entity.User;
import com.minhdan.english_practice_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Register/login with device ID (Android ID).
     * Public endpoint — no auth required.
     */
    @PostMapping("/register-device")
    public ResponseEntity<AuthResponse> registerDevice(@RequestBody RegisterDeviceRequest request) {
        AuthResponse response = userService.registerWithDeviceId(request.getDeviceId());
        return ResponseEntity.ok(response);
    }

    /**
     * Link Google account to current device user.
     * Protected endpoint — requires valid access token.
     */
    @PostMapping("/link-google")
    public ResponseEntity<AuthResponse> linkGoogle(
            @AuthenticationPrincipal User currentUser,
            @RequestBody LinkGoogleRequest request) {
        AuthResponse response = userService.linkGoogle(currentUser, request.getGoogleIdToken());
        return ResponseEntity.ok(response);
    }

    /**
     * Unlink Google account — revert to anonymous device user.
     * Protected endpoint — requires valid access token.
     */
    @PostMapping("/unlink-google")
    public ResponseEntity<AuthResponse> unlinkGoogle(@AuthenticationPrincipal User currentUser) {
        AuthResponse response = userService.unlinkGoogle(currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Check if a Google account is already linked to an existing user.
     * Protected endpoint.
     */
    @PostMapping("/check-google")
    public ResponseEntity<CheckGoogleResponse> checkGoogle(@RequestBody LinkGoogleRequest request) {
        CheckGoogleResponse response = userService.checkGoogle(request.getGoogleIdToken());
        return ResponseEntity.ok(response);
    }

    /**
     * Switch to an existing Google-linked account.
     * Protected endpoint.
     */
    @PostMapping("/switch-to-google")
    public ResponseEntity<AuthResponse> switchToGoogle(
            @AuthenticationPrincipal User currentUser,
            @RequestBody LinkGoogleRequest request) {
        AuthResponse response = userService.switchToGoogleAccount(currentUser, request.getGoogleIdToken());
        return ResponseEntity.ok(response);
    }

    /**
     * Create a new account with Google info, abandoning current device account.
     * Protected endpoint.
     */
    @PostMapping("/create-with-google")
    public ResponseEntity<AuthResponse> createWithGoogle(
            @AuthenticationPrincipal User currentUser,
            @RequestBody LinkGoogleRequest request) {
        AuthResponse response = userService.createWithGoogle(currentUser, request.getGoogleIdToken());
        return ResponseEntity.ok(response);
    }

    /**
     * Exchange Firebase ID token for app JWT pair (LEGACY — backward compat).
     * Public endpoint — no auth required.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        AuthResponse response = userService.registerWithFirebaseToken(request.getFirebaseToken());
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh expired access token using refresh token.
     * Public endpoint — no auth required.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        AuthResponse response = userService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    /**
     * Get current user info.
     * Protected endpoint — requires valid access token.
     */
    @PostMapping("/get-info")
    public ResponseEntity<UserResponse> getInfo(@AuthenticationPrincipal User currentUser) {
        UserResponse response = userService.getCurrentUserResponse(currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Backward-compatible GET endpoint.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal User currentUser) {
        UserResponse response = userService.getCurrentUserResponse(currentUser);
        return ResponseEntity.ok(response);
    }
}
