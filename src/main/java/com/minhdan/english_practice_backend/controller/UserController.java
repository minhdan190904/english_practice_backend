package com.minhdan.english_practice_backend.controller;

import com.minhdan.english_practice_backend.dto.response.UserResponse;
import com.minhdan.english_practice_backend.entity.User;
import com.minhdan.english_practice_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal User currentUser) {
        UserResponse response = userService.getCurrentUserResponse(currentUser);
        return ResponseEntity.ok(response);
    }
}
