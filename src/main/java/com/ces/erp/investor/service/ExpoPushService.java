package com.ces.erp.investor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Expo Push API ilə bildiriş göndərir (https://exp.host/--/api/v2/push/send).
 * Best-effort: heç vaxt exception atmır — push uğursuzluğu çağırış axınını pozmamalıdır.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExpoPushService {

    private static final String EXPO_URL = "https://exp.host/--/api/v2/push/send";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    // Fire-and-forget — çağırış axınını (ERP transaction) heç vaxt bloklamır.
    public void send(List<String> tokens, String title, String body, Map<String, Object> data) {
        if (tokens == null || tokens.isEmpty()) return;
        CompletableFuture.runAsync(() -> doSend(tokens, title, body, data));
    }

    private void doSend(List<String> tokens, String title, String body, Map<String, Object> data) {
        try {
            List<Map<String, Object>> messages = new ArrayList<>();
            for (String token : tokens) {
                if (token == null || token.isBlank()) continue;
                Map<String, Object> msg = new HashMap<>();
                msg.put("to", token);
                msg.put("title", title);
                msg.put("body", body);
                msg.put("sound", "default");
                msg.put("channelId", "default");
                if (data != null) msg.put("data", data);
                messages.add(msg);
            }
            if (messages.isEmpty()) return;

            String json = objectMapper.writeValueAsString(messages);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(EXPO_URL))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                log.warn("[ExpoPush] status {} — {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.warn("[ExpoPush] göndərmə uğursuz: {}", e.getMessage());
        }
    }
}
