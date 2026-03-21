package com.ces.erp.common.trash.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrashItem {

    private Long id;
    private String entityType;
    private String entityLabel;
    private String moduleCode;
    private String moduleName;
    private LocalDateTime deletedAt;
    private Map<String, String> details;
}
