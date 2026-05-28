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
public class QuestionDto {
    private Integer id;
    private String type;
    private String question;
    private String sentence;
    private List<String> options;
    private List<String> words;
    private Integer correctAnswer;
    private List<Integer> correctOrder;
    private String correctedSentence;
    private String correctSentence;
    private String explanation;
}
