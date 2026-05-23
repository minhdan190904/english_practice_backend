package com.minhdan.english_practice_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Sense {
    private String definition;
    @JsonProperty("definition_vi")
    private String definitionVi;
    @JsonProperty("short_meaning_vi")
    private String shortMeaningVi;
    private List<Example> examples;
}
