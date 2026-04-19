package com.ces.erp.accounting.entity;

import com.ces.erp.common.entity.BaseEntity;
import com.ces.erp.customer.entity.Customer;
import com.ces.erp.enums.DocumentType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "generated_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratedDocument extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String documentNumber;   // "0001", "0002" ...

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;

    @Column(nullable = false)
    @Builder.Default
    private LocalDate documentDate = LocalDate.now();

    // ─── Müştəri əlaqəsi ──────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    // ─── Müştəri snapshot-u ───────────────────────────────────────────────────

    @Column(nullable = false)
    private String customerName;

    @Column(length = 20)
    private String customerVoen;

    @Column(columnDefinition = "TEXT")
    private String customerAddress;

    @Column(length = 150)
    private String customerDirectorName;

    // ─── Müqavilə sahələri (Hesab-Faktura üçün) ───────────────────────────────

    private LocalDate contractDate;

    @Column(length = 100)
    private String contractNumber;

    // ─── Maliyyə ──────────────────────────────────────────────────────────────

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal vatRate = BigDecimal.ZERO;  // konfiqurasiyadan götürülür

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal grandTotal = BigDecimal.ZERO;

    // ─── Seçilmiş bank məlumatları (sənəd yaradılarkən snapshot) ─────────────

    @Column(length = 200)
    private String bankName;

    @Column(length = 50)
    private String bankCode;

    @Column(length = 20)
    private String bankSwift;

    @Column(length = 50)
    private String bankIban;

    @Column(length = 50)
    private String bankMh;

    @Column(length = 50)
    private String bankHh;

    // ─── PDF yolu ─────────────────────────────────────────────────────────────

    @Column
    private String pdfFilePath;

    // ─── Mənbə qaimə ID-ləri (traceability) ──────────────────────────────────

    @Column(columnDefinition = "TEXT")
    private String sourceInvoiceIds;  // JSON array: "[1,2,3]"

    @Column(columnDefinition = "TEXT")
    private String addendumNumbers;   // JSON array: "[1,2,3]"

    @Column(columnDefinition = "TEXT")
    private String notes;

    // ─── Sətir əlaqəsi ────────────────────────────────────────────────────────

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DocumentLine> lines = new ArrayList<>();
}
