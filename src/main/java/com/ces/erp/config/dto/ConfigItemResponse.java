package com.ces.erp.config.dto;

import com.ces.erp.config.entity.ConfigItem;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConfigItemResponse {

    private Long id;
    private String category;
    private String key;
    private String value;
    private String description;
    private int sortOrder;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ConfigItemResponse from(ConfigItem entity) {
        return ConfigItemResponse.builder()
                .id(entity.getId())
                .category(entity.getCategory())
                .key(entity.getKey())
                .value(entity.getValue())
                .description(entity.getDescription())
                .sortOrder(entity.getSortOrder())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
