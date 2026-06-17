package com.ces.erp.accounting.dto;

import com.ces.erp.coordinator.entity.CoordinatorPlanItem;
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

    // Çoxlu texnika: layihənin BÜTÜN texnika xətləri (sənədi olmasa da görünsün)
    private List<LineInfo> equipmentLines;

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
        private Long planItemId;        // hansı texnika xəttinə aiddir (null = sorğu səviyyəsi)
        private String equipmentName;   // xəttin texnikası (görünüş üçün)
    }

    @Data
    @Builder
    public static class LineInfo {
        private Long planItemId;
        private String equipmentName;
        private String equipmentCode;
    }

    public static RequestDocumentCheckResponse from(TechRequest r, List<RequestDocument> docs) {
        return from(r, docs, null);
    }

    public static RequestDocumentCheckResponse from(TechRequest r, List<RequestDocument> docs,
                                                    List<CoordinatorPlanItem> lines) {
        List<DocumentInfo> docInfos = docs.stream()
                .map(d -> DocumentInfo.builder()
                        .id(d.getId())
                        .docType(d.getDocType())
                        .fileName(d.getFileName())
                        .uploadedAt(d.getCreatedAt())
                        .uploadedByName(d.getUploadedBy() != null ? d.getUploadedBy().getFullName() : null)
                        .planItemId(d.getPlanItem() != null ? d.getPlanItem().getId() : null)
                        .equipmentName(d.getPlanItem() != null && d.getPlanItem().getEquipment() != null
                                ? d.getPlanItem().getEquipment().getName() : null)
                        .build())
                .toList();
        boolean hasContract = docs.stream().anyMatch(d -> d.getDocType() == RequestDocumentType.CONTRACT);
        boolean hasProtocol = docs.stream().anyMatch(d -> d.getDocType() == RequestDocumentType.PRICE_PROTOCOL);
        List<LineInfo> lineInfos = lines == null ? List.of() : lines.stream()
                .map(it -> LineInfo.builder()
                        .planItemId(it.getId())
                        .equipmentName(it.getEquipment() != null ? it.getEquipment().getName() : null)
                        .equipmentCode(it.getEquipment() != null ? it.getEquipment().getEquipmentCode() : null)
                        .build())
                .toList();
        return RequestDocumentCheckResponse.builder()
                .requestId(r.getId())
                .requestCode(r.getRequestCode())
                .status(r.getStatus())
                .companyName(r.getCompanyName())
                .projectName(r.getProjectName())
                .region(r.getRegion())
                .agreedTotalPrice(r.getAgreedTotalPrice())
                .documents(docInfos)
                .equipmentLines(lineInfos)
                .contractUploaded(hasContract)
                .priceProtocolUploaded(hasProtocol)
                .build();
    }
}
