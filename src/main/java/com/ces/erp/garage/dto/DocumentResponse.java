package com.ces.erp.garage.dto;

import com.ces.erp.garage.entity.EquipmentDocument;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentResponse {

    private Long id;
    private String documentName;
    private String documentType;
    private String filePath;
    private String fileType;
    private Long uploadedByUserId;
    private String uploadedByUserName;
    private LocalDateTime createdAt;

    public static DocumentResponse from(EquipmentDocument d) {
        return DocumentResponse.builder()
                .id(d.getId())
                .documentName(d.getDocumentName())
                .documentType(d.getDocumentType())
                .filePath(d.getFilePath())
                .fileType(d.getFileType())
                .uploadedByUserId(d.getUploadedBy() != null ? d.getUploadedBy().getId() : null)
                .uploadedByUserName(d.getUploadedBy() != null ? d.getUploadedBy().getFullName() : null)
                .createdAt(d.getCreatedAt())
                .build();
    }
}
