package com.minhdan.english_practice_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Example {
    private String cf;
    private String x;
}
