package com.ces.erp.customer.dto;

import com.ces.erp.customer.entity.CustomerDocument;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CustomerDocumentResponse {

    private Long id;
    private String documentName;
    private String fileType;
    private String uploadedByUserName;
    private LocalDateTime createdAt;

    public static CustomerDocumentResponse from(CustomerDocument d) {
        return CustomerDocumentResponse.builder()
                .id(d.getId())
                .documentName(d.getDocumentName())
                .fileType(d.getFileType())
                .uploadedByUserName(d.getUploadedBy() != null ? d.getUploadedBy().getFullName() : null)
                .createdAt(d.getCreatedAt())
                .build();
    }
}
