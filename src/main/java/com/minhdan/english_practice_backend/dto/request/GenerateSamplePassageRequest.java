package com.minhdan.english_practice_backend.dto.request;

import lombok.Data;

@Data
public class GenerateSamplePassageRequest {
    private String category;
    private String level;
    private Integer minWords;
    private Integer maxWords;
}
