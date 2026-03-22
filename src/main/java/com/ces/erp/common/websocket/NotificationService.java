package com.ces.erp.common.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void send(String title, String message, String module, String type) {
        NotificationPayload payload = NotificationPayload.builder()
                .type(type)
                .title(title)
                .message(message)
                .module(module)
                .timestamp(LocalDateTime.now())
                .build();
        messagingTemplate.convertAndSend("/topic/notifications", payload);
    }

    public void info(String title, String message, String module) {
        send(title, message, module, "INFO");
    }

    public void success(String title, String message, String module) {
        send(title, message, module, "SUCCESS");
    }
}
