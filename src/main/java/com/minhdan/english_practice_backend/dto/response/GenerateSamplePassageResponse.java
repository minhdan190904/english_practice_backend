package com.minhdan.english_practice_backend.dto.response;

import com.minhdan.english_practice_backend.dto.SelectedWordDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateSamplePassageResponse {
    private String title;
    private String passage;
    private String passageVi;
    private List<SelectedWordDto> selectedWords;
    private String category;
    private String level;
    private int wordCount;
    /** Base64-encoded PNG from Imagen. Null if image generation failed. */
    private String imageBase64;
}
