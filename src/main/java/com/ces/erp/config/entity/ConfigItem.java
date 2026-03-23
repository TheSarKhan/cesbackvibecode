package com.ces.erp.config.entity;

import com.ces.erp.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "config_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"category", "item_key"}),
        indexes = {
                @Index(name = "idx_config_category", columnList = "category"),
                @Index(name = "idx_config_deleted", columnList = "deleted"),
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigItem extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String category;       // e.g. "EQUIPMENT_BRAND", "EQUIPMENT_TYPE", "REGION"

    @Column(name = "item_key", nullable = false, length = 200)
    private String key;            // e.g. "CAT", "Komatsu", "Bakı"

    @Column(length = 500)
    private String value;          // optional display label or extra data

    @Column(length = 1000)
    private String description;    // optional note

    @Column(nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
