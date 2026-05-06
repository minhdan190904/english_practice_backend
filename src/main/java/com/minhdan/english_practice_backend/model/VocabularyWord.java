package com.minhdan.english_practice_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VocabularyWord {
    private String word;
    private String level;
    private String category;
    private String pos;
    private String phonetic;
    @JsonProperty("phonetic_text")
    private String phoneticText;
    @JsonProperty("phonetic_am")
    private String phoneticAm;
    @JsonProperty("phonetic_am_text")
    private String phoneticAmText;
    private List<Sense> senses;
}
