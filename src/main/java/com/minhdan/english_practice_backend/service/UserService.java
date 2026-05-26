package com.minhdan.english_practice_backend.service;

import com.minhdan.english_practice_backend.dto.response.AuthResponse;
import com.minhdan.english_practice_backend.dto.response.CheckGoogleResponse;
import com.minhdan.english_practice_backend.dto.response.UserResponse;
import com.minhdan.english_practice_backend.entity.User;
import com.google.firebase.auth.FirebaseToken;

public interface UserService {
    User getOrCreateUserFromToken(FirebaseToken token);
    UserResponse getCurrentUserResponse(User currentUser);
    AuthResponse registerWithFirebaseToken(String firebaseIdToken);
    AuthResponse registerWithDeviceId(String deviceId);
    AuthResponse linkGoogle(User currentUser, String googleIdToken);
    AuthResponse unlinkGoogle(User currentUser);
    AuthResponse refreshToken(String refreshToken);
    CheckGoogleResponse checkGoogle(String googleIdToken);
    AuthResponse switchToGoogleAccount(User currentUser, String googleIdToken);
    AuthResponse createWithGoogle(User currentUser, String googleIdToken);
}
