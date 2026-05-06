package com.minhdan.english_practice_backend.controller;

import com.minhdan.english_practice_backend.dto.request.GenerateSamplePassageRequest;
import com.minhdan.english_practice_backend.dto.response.GenerateSamplePassageResponse;
import com.minhdan.english_practice_backend.dto.SelectedWordDto;
import com.minhdan.english_practice_backend.service.SampleWordSelectorService;
import com.minhdan.english_practice_backend.service.VertexSamplePassageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class SamplePassageController {

    private final SampleWordSelectorService wordSelectorService;
    private final VertexSamplePassageService passageService;

    @PostMapping("/sample-passage")
    public ResponseEntity<GenerateSamplePassageResponse> generateSamplePassage(@RequestBody GenerateSamplePassageRequest request) {
        String category = request.getCategory() != null ? request.getCategory() : "technology";
        String level = request.getLevel() != null ? request.getLevel() : "A1";
        int minWords = request.getMinWords() != null ? request.getMinWords() : 80;
        int maxWords = request.getMaxWords() != null ? request.getMaxWords() : 140;

        List<SelectedWordDto> selectedWords = wordSelectorService.selectWords(category, level);

        GenerateSamplePassageResponse response = passageService.generatePassage(category, level, minWords, maxWords, selectedWords);

        return ResponseEntity.ok(response);
    }
}
