package com.ces.erp.permission.dto;

import com.ces.erp.permission.entity.Permission;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PermissionResponse {
    private Long id;
    private String code;
    private String moduleCode;
    private String moduleNameAz;
    private String action;
    private String labelAz;
    private boolean autoDiscovered;

    public static PermissionResponse from(Permission p, String moduleNameAz) {
        return PermissionResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .moduleCode(p.getModuleCode())
                .moduleNameAz(moduleNameAz != null ? moduleNameAz : p.getModuleCode())
                .action(p.getAction())
                .labelAz(p.getLabelAz())
                .autoDiscovered(p.isAutoDiscovered())
                .build();
    }
}
