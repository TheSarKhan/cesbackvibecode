package com.ces.erp.operator.dto;

import com.ces.erp.enums.OperatorDocumentType;
import com.ces.erp.operator.entity.OperatorDocument;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OperatorDocumentResponse {

    private Long id;
    private OperatorDocumentType documentType;
    private String fileName;
    private LocalDateTime uploadedAt;

    public static OperatorDocumentResponse from(OperatorDocument d) {
        return OperatorDocumentResponse.builder()
                .id(d.getId())
                .documentType(d.getDocumentType())
                .fileName(d.getFileName())
                .uploadedAt(d.getUploadedAt())
                .build();
    }
}
