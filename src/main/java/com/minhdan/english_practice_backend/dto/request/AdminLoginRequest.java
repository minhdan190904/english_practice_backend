package com.minhdan.english_practice_backend.dto.request;

import lombok.Data;

@Data
public class AdminLoginRequest {
    private String username;
    private String password;
}
