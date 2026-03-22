package com.ces.erp.common.websocket;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class NotificationPayload {
    private String type;       // "INFO", "SUCCESS", "WARNING"
    private String title;      // e.g. "Yeni sorğu yaradıldı"
    private String message;    // e.g. "REQ-0042 koordinatora göndərildi"
    private String module;     // "REQUESTS", "ACCOUNTING", etc.
    private LocalDateTime timestamp;
}
