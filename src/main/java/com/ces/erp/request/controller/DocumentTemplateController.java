package com.ces.erp.request.controller;

import com.ces.erp.accounting.service.DocumentCheckService;
import com.ces.erp.common.dto.ApiResponse;
import com.ces.erp.common.security.UserPrincipal;
import com.ces.erp.coordinator.service.CoordinatorPlanService;
import com.ces.erp.request.entity.RequestDocumentType;
import com.ces.erp.request.service.DocumentTemplatePdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/api/requests/{requestId}/templates")
@RequiredArgsConstructor
@Tag(name = "Document Templates", description = "Avtomatik sənəd və akt PDF generasiyası")
public class DocumentTemplateController {

    private final DocumentTemplatePdfService templatePdfService;
    private final CoordinatorPlanService coordinatorPlanService;
    private final DocumentCheckService documentCheckService;

    @GetMapping("/handover-act/pdf")
    @Operation(summary = "Təhvil-təslim aktı PDF faylını generasiya et və yüklə")
    public ResponseEntity<byte[]> downloadHandoverAct(@PathVariable Long requestId) {
        byte[] pdfBytes = templatePdfService.generateHandoverActPdf(requestId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"Tehvil_Teslim_Akt_" + requestId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/price-protocol/pdf")
    @Operation(summary = "Qiymət razılaşma protokolu PDF faylını generasiya et və yüklə")
    public ResponseEntity<byte[]> downloadPriceProtocol(@PathVariable Long requestId) {
        byte[] pdfBytes = templatePdfService.generatePriceProtocolPdf(requestId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"Qiymet_Protokolu_" + requestId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @PostMapping("/auto-attach-handover-act")
    @Operation(summary = "Təhvil-təslim aktını avtomatik generasiya edib Koordinator planına bağla")
    public ResponseEntity<ApiResponse<String>> autoAttachHandoverAct(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal principal) {
        byte[] pdfBytes = templatePdfService.generateHandoverActPdf(requestId);
        MultipartFile multipartFile = new InMemoryMultipartFile(
                "file",
                "Tehvil_Teslim_Akt_" + requestId + ".pdf",
                MediaType.APPLICATION_PDF_VALUE,
                pdfBytes
        );
        coordinatorPlanService.uploadDocument(
                requestId,
                multipartFile,
                "Təhvil-Təslim Aktı (Sistem Generasiyası)",
                "HANDOVER_ACT",
                principal != null ? principal.getId() : null
        );
        return ResponseEntity.ok(ApiResponse.success("Təhvil-təslim aktı avtomatik yaradıldı və plana bağlandı", "OK"));
    }

    @PostMapping("/auto-attach-price-protocol")
    @Operation(summary = "Qiymət protokolunu avtomatik generasiya edib Mühasibatlığa bağla")
    public ResponseEntity<ApiResponse<String>> autoAttachPriceProtocol(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal principal) {
        byte[] pdfBytes = templatePdfService.generatePriceProtocolPdf(requestId);
        MultipartFile multipartFile = new InMemoryMultipartFile(
                "file",
                "Qiymet_Protokolu_" + requestId + ".pdf",
                MediaType.APPLICATION_PDF_VALUE,
                pdfBytes
        );
        documentCheckService.uploadDocument(
                requestId,
                RequestDocumentType.PRICE_PROTOCOL,
                multipartFile,
                principal != null ? principal.getId() : null
        );
        return ResponseEntity.ok(ApiResponse.success("Qiymət protokolu avtomatik yaradıldı və sənədlərə bağlandı", "OK"));
    }

    public static class InMemoryMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public InMemoryMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content != null ? content : new byte[0];
        }

        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return originalFilename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() { return content; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(content); }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(content);
            }
        }
    }
}
