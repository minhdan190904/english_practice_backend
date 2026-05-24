package com.minhdan.english_practice_backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Service
public class TelegramService {

    private final RestClient restClient;

    @Value("${telegram.bot-token}")
    private String botToken;

    @Value("${telegram.chat-id}")
    private String chatId;

    public TelegramService() {
        this.restClient = RestClient.create();
    }

    public void sendMessage(String text) {
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "chat_id", chatId,
                            "text", text,
                            "parse_mode", "HTML"
                    ))
                    .retrieve()
                    .body(String.class);
            log.info("Telegram message sent successfully");
        } catch (Exception e) {
            log.error("Failed to send Telegram message", e);
        }
    }
}
