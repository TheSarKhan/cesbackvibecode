package com.ces.erp.accounting.dto;

import com.ces.erp.enums.DocumentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class GeneratedDocumentRequest {

    @NotNull(message = "Sənəd növü mütləqdir")
    private DocumentType documentType;

    @NotNull(message = "Müştəri ID mütləqdir")
    private Long customerId;

    private LocalDate contractDate;
    private String contractNumber;
    private String notes;

    /** Traceability: hansı qaimələrdən yaradıldı */
    private List<Long> sourceInvoiceIds;

    /** Əlavə nömrələri (Təhvil-Təslim Aktı üçün) */
    private List<Integer> addendumNumbers;

    /** Seçilmiş bank məlumatları */
    private String bankName;
    private String bankCode;
    private String bankSwift;
    private String bankIban;
    private String bankMh;
    private String bankHh;

    @NotNull(message = "Sətir siyahısı boş ola bilməz")
    @NotEmpty(message = "Ən azı bir sətir lazımdır")
    @Valid
    private List<DocumentLineRequest> lines;
}
