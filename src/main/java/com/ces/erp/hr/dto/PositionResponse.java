package com.ces.erp.hr.dto;

import com.ces.erp.hr.entity.Position;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PositionResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal defaultSalary;
    private Long departmentId;
    private String departmentName;
    private boolean active;

    public static PositionResponse from(Position p) {
        return PositionResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .defaultSalary(p.getDefaultSalary())
                .departmentId(p.getDepartment() != null ? p.getDepartment().getId() : null)
                .departmentName(p.getDepartment() != null ? p.getDepartment().getName() : null)
                .active(p.isActive())
                .build();
    }
}
