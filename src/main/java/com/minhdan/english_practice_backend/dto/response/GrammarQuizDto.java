package com.minhdan.english_practice_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrammarQuizDto {
    private Integer topicId;
    private List<QuestionDto> questions;
}
