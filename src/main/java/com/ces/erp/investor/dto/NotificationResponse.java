package com.ces.erp.investor.dto;

import com.ces.erp.investor.entity.InvestorNotification;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {

    private Long id;
    private String title;
    private String body;
    private String type;
    private Long relatedId;
    private boolean read;
    private LocalDateTime createdAt;

    public static NotificationResponse from(InvestorNotification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .body(n.getBody())
                .type(n.getType())
                .relatedId(n.getRelatedId())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
