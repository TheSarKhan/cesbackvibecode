package com.ces.erp.department.dto;

import com.ces.erp.department.entity.Department;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DepartmentResponse {

    private Long id;
    private String name;
    private String description;
    private boolean active;
    private LocalDateTime createdAt;

    public static DepartmentResponse from(Department d) {
        return DepartmentResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .description(d.getDescription())
                .active(d.isActive())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
