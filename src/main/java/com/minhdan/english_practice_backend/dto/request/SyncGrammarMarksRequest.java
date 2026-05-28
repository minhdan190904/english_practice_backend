package com.minhdan.english_practice_backend.dto.request;

import lombok.Data;
import java.util.Map;

@Data
public class SyncGrammarMarksRequest {
    private Map<String, Boolean> marks;
}
