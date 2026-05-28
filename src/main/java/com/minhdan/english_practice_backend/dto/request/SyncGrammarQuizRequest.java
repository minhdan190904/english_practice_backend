package com.minhdan.english_practice_backend.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class SyncGrammarQuizRequest {
    private List<Integer> completedTopicIds;
}
