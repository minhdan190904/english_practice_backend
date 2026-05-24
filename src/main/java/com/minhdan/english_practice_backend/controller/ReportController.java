package com.minhdan.english_practice_backend.controller;

import com.minhdan.english_practice_backend.dto.ReportRequest;
import com.minhdan.english_practice_backend.service.TelegramService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
public class ReportController {

    private final TelegramService telegramService;

    @PostMapping
    public ResponseEntity<Map<String, String>> submitReport(@RequestBody ReportRequest request) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        String message = String.format(
            "\uD83D\uDEA8 <b>New User Report</b>\n" +
            "\uD83D\uDCC5 Time: %s\n" +
            "\uD83C\uDFF7 Type: %s\n" +
            "\uD83D\uDCCC Content: %s\n" +
            "\uD83D\uDCAC Reason: %s\n" +
            "\uD83D\uDC64 User: %s",
            timestamp,
            request.getType() != null ? request.getType() : "unknown",
            request.getContent() != null ? request.getContent() : "-",
            request.getReason() != null ? request.getReason() : "-",
            request.getUserId() != null ? request.getUserId() : "anonymous"
        );

        telegramService.sendMessage(message);

        return ResponseEntity.ok(Map.of("status", "success", "message", "Report submitted successfully"));
    }
}
