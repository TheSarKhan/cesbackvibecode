package com.ces.erp.technicalservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceChecklistItemDto {
    private Long id;
    private String itemName;
    private boolean checked;
    private String note;
}
