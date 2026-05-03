package com.minhdan.english_practice_backend.service;

import com.minhdan.english_practice_backend.dto.response.UserResponse;
import com.minhdan.english_practice_backend.entity.User;
import com.google.firebase.auth.FirebaseToken;

public interface UserService {
    User getOrCreateUserFromToken(FirebaseToken token);
    UserResponse getCurrentUserResponse(User currentUser);
}
