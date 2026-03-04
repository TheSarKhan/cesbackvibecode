package com.ces.erp.systemmodule.dto;

import com.ces.erp.systemmodule.entity.SystemModule;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SystemModuleResponse {

    private Long id;
    private String code;
    private String nameAz;
    private String nameEn;
    private Integer orderIndex;

    public static SystemModuleResponse from(SystemModule m) {
        return SystemModuleResponse.builder()
                .id(m.getId())
                .code(m.getCode())
                .nameAz(m.getNameAz())
                .nameEn(m.getNameEn())
                .orderIndex(m.getOrderIndex())
                .build();
    }
}
