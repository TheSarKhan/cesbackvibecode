package com.ces.erp.accounting.dto;

import com.ces.erp.enums.RequestStatus;
import com.ces.erp.request.entity.RequestDocument;
import com.ces.erp.request.entity.RequestDocumentType;
import com.ces.erp.request.entity.TechRequest;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RequestDocumentCheckResponse {

    private Long requestId;
    private String requestCode;
    private RequestStatus status;
    private String companyName;
    private String projectName;
    private String region;
    private BigDecimal agreedTotalPrice;

    private List<DocumentInfo> documents;

    private boolean contractUploaded;
    private boolean priceProtocolUploaded;

    @Data
    @Builder
    public static class DocumentInfo {
        private Long id;
        private RequestDocumentType docType;
        private String fileName;
        private LocalDateTime uploadedAt;
        private String uploadedByName;
    }

    public static RequestDocumentCheckResponse from(TechRequest r, List<RequestDocument> docs) {
        List<DocumentInfo> docInfos = docs.stream()
                .map(d -> DocumentInfo.builder()
                        .id(d.getId())
                        .docType(d.getDocType())
                        .fileName(d.getFileName())
                        .uploadedAt(d.getCreatedAt())
                        .uploadedByName(d.getUploadedBy() != null ? d.getUploadedBy().getFullName() : null)
                        .build())
                .toList();
        boolean hasContract = docs.stream().anyMatch(d -> d.getDocType() == RequestDocumentType.CONTRACT);
        boolean hasProtocol = docs.stream().anyMatch(d -> d.getDocType() == RequestDocumentType.PRICE_PROTOCOL);
        return RequestDocumentCheckResponse.builder()
                .requestId(r.getId())
                .requestCode(r.getRequestCode())
                .status(r.getStatus())
                .companyName(r.getCompanyName())
                .projectName(r.getProjectName())
                .region(r.getRegion())
                .agreedTotalPrice(r.getAgreedTotalPrice())
                .documents(docInfos)
                .contractUploaded(hasContract)
                .priceProtocolUploaded(hasProtocol)
                .build();
    }
}
