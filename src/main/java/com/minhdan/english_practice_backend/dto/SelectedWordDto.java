package com.minhdan.english_practice_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectedWordDto {
    private String word;
    private String level;
    private String category;
    private String pos;
    private String definition;
    private String example;
    private String phoneticText;
    private String phoneticAmText;
}
