package com.ces.erp.accounting.service;

import com.ces.erp.accounting.dto.*;
import com.ces.erp.accounting.entity.DocumentLine;
import com.ces.erp.accounting.entity.GeneratedDocument;
import com.ces.erp.accounting.entity.Invoice;
import com.ces.erp.accounting.repository.GeneratedDocumentRepository;
import com.ces.erp.accounting.repository.InvoiceRepository;
import com.ces.erp.common.audit.AuditService;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.FileStorageException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.common.service.FileStorageService;
import com.ces.erp.config.dto.ConfigItemResponse;
import com.ces.erp.config.service.ConfigService;
import com.ces.erp.coordinator.entity.CoordinatorPlan;
import com.ces.erp.coordinator.repository.CoordinatorPlanRepository;
import com.ces.erp.customer.entity.Customer;
import com.ces.erp.customer.repository.CustomerRepository;
import com.ces.erp.enums.DocumentType;
import com.ces.erp.enums.InvoiceStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeneratedDocumentService {

    private final GeneratedDocumentRepository documentRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final CoordinatorPlanRepository coordinatorPlanRepository;
    private final ConfigService configService;
    private final PdfGenerationService pdfGenerationService;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;

    // ─── Siyahı ───────────────────────────────────────────────────────────────

    public PagedResponse<GeneratedDocumentResponse> getAllPaged(int page, int size, String search, String type) {
        String q = (search != null && !search.isBlank()) ? search : null;
        DocumentType typeEnum = null;
        if (type != null && !type.isBlank()) {
            try { typeEnum = DocumentType.valueOf(type); } catch (IllegalArgumentException ignored) {}
        }
        var pageable = PageRequest.of(page, size);
        return PagedResponse.from(
                documentRepository.findAllFiltered(q, typeEnum, pageable),
                GeneratedDocumentResponse::from
        );
    }

    // ─── ID ilə gətir ─────────────────────────────────────────────────────────

    public GeneratedDocumentResponse getById(Long id) {
        return GeneratedDocumentResponse.from(findOrThrow(id));
    }

    // ─── Yarat ────────────────────────────────────────────────────────────────

    @Transactional
    public GeneratedDocumentResponse create(GeneratedDocumentRequest req) {
        // 1. Müştəri yüklə + snapshot
        Customer customer = customerRepository.findByIdAndDeletedFalse(req.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Müştəri", req.getCustomerId()));

        // 2. ƏDV rate konfiqurasiyadan
        BigDecimal vatRate = getVatRate();

        // 3. Sətirləri yarat
        List<DocumentLine> lines = new ArrayList<>();
        int order = 1;
        BigDecimal subtotal = BigDecimal.ZERO;
        for (DocumentLineRequest lr : req.getLines()) {
            BigDecimal totalPrice = lr.getQuantity().multiply(lr.getUnitPrice())
                    .setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(totalPrice);

            DocumentLine line = DocumentLine.builder()
                    .lineOrder(order++)
                    .description(lr.getDescription())
                    .unit(lr.getUnit())
                    .quantity(lr.getQuantity())
                    .unitPrice(lr.getUnitPrice())
                    .totalPrice(totalPrice)
                    .sourceInvoiceId(lr.getSourceInvoiceId())
                    .build();
            lines.add(line);
        }

        // 4. ƏDV + yekun
        BigDecimal vatAmount = subtotal.multiply(vatRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = subtotal.add(vatAmount);

        // 5. Auto-nömrə
        int maxNum = documentRepository.findMaxDocumentNumber();
        String docNumber = String.format("%04d", maxNum + 1);

        // 6. Mənbə ID-ləri + əlavə nömrələri JSON array
        String sourceIdsJson = null;
        if (req.getSourceInvoiceIds() != null && !req.getSourceInvoiceIds().isEmpty()) {
            sourceIdsJson = "[" + req.getSourceInvoiceIds().toString().replaceAll("[\\[\\] ]", "") + "]";
        }
        String addendumJson = null;
        if (req.getAddendumNumbers() != null && !req.getAddendumNumbers().isEmpty()) {
            addendumJson = "[" + req.getAddendumNumbers().toString().replaceAll("[\\[\\] ]", "") + "]";
        }

        // 7. Entity yarat
        GeneratedDocument doc = GeneratedDocument.builder()
                .documentNumber(docNumber)
                .documentType(req.getDocumentType())
                .customer(customer)
                .customerName(customer.getCompanyName())
                .customerVoen(customer.getVoen())
                .customerAddress(customer.getAddress())
                .customerDirectorName(customer.getDirectorName())
                .contractDate(req.getContractDate())
                .contractNumber(req.getContractNumber())
                .subtotal(subtotal)
                .vatRate(vatRate)
                .vatAmount(vatAmount)
                .grandTotal(grandTotal)
                .sourceInvoiceIds(sourceIdsJson)
                .addendumNumbers(addendumJson)
                .bankName(req.getBankName())
                .bankCode(req.getBankCode())
                .bankSwift(req.getBankSwift())
                .bankIban(req.getBankIban())
                .bankMh(req.getBankMh())
                .bankHh(req.getBankHh())
                .notes(req.getNotes())
                .build();

        GeneratedDocument saved = documentRepository.save(doc);

        // 8. Sətirləri sənədə bağla və əlavə et
        lines.forEach(l -> {
            l.setDocument(saved);
            saved.getLines().add(l);
        });
        documentRepository.save(saved);

        // 9. PDF generasiya
        try {
            String pdfPath = pdfGenerationService.generateAndStore(saved);
            saved.setPdfFilePath(pdfPath);
            documentRepository.save(saved);
        } catch (Exception e) {
            log.error("PDF yaradıla bilmədi: {}", e.getMessage(), e);
        }

        // 10. Audit
        auditService.log("SƏNƏD", saved.getId(), saved.getDocumentNumber(),
                "YARADILDI", "Sənəd " + req.getDocumentType() + " — " + customer.getCompanyName());

        return GeneratedDocumentResponse.from(saved);
    }

    // ─── Qaimələrdən ön baxış sətirləri ──────────────────────────────────────

    @Transactional
    public List<DocumentLineRequest> previewFromInvoices(List<Long> invoiceIds) {
        List<DocumentLineRequest> result = new ArrayList<>();

        for (Long invId : invoiceIds) {
            Invoice invoice = invoiceRepository.findByIdActive(invId)
                    .orElseThrow(() -> new ResourceNotFoundException("Qaimə", invId));

            if (invoice.getStatus() != InvoiceStatus.APPROVED) {
                throw new BusinessException("Qaimə (#" + invId + ") təsdiqlənməyib. Yalnız APPROVED qaimələr istifadə edilə bilər.");
            }

            // Qaimənin daşınma sətirləri (həm ilk, həm proses daşınmaları)
            List<com.ces.erp.accounting.entity.InvoiceTransport> invTransports = invoice.getTransports().stream()
                    .filter(t -> !t.isDeleted())
                    .toList();
            BigDecimal transportTotal = invTransports.stream()
                    .map(t -> t.getTransportAmount() != null ? t.getTransportAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Toplu qaimə sətirləri (hər texnika ayrı)
            List<com.ces.erp.accounting.entity.InvoiceLine> invLines = invoice.getLines() == null
                    ? java.util.List.of()
                    : invoice.getLines().stream().filter(l -> !l.isDeleted()).toList();

            if (!invLines.isEmpty()) {
                // Texnika ID → ad (daşınma sətrində texnikanı göstərmək üçün)
                java.util.Map<Long, String> eqNames = new java.util.LinkedHashMap<>();
                for (var l : invLines) {
                    Long eid = l.getEquipment() != null ? l.getEquipment().getId() : null;
                    if (eid != null && l.getEquipmentName() != null) eqNames.put(eid, l.getEquipmentName());
                }

                // Hər texnika üçün ayrıca icarə sətri (daşınmasız xalis məbləğlə)
                for (var l : invLines) {
                    DocumentLineRequest eqLine = new DocumentLineRequest();
                    eqLine.setDescription(buildLineDescription(l, invoice));
                    eqLine.setUnit("ədəd");
                    eqLine.setQuantity(BigDecimal.ONE);
                    BigDecimal amt = l.getEquipmentAmount() != null ? l.getEquipmentAmount()
                            : (l.getLineTotal() != null
                                ? l.getLineTotal().subtract(l.getTransportAmount() != null ? l.getTransportAmount() : BigDecimal.ZERO)
                                : BigDecimal.ZERO);
                    eqLine.setUnitPrice(amt.compareTo(BigDecimal.ZERO) > 0 ? amt : BigDecimal.ZERO);
                    eqLine.setSourceInvoiceId(invId);
                    result.add(eqLine);
                }

                // Daşınma — hər texnika üçün ayrıca, YALNIZ qaimədə fakturalanmış məbləğdən:
                // 1) InvoiceTransport (ilk + proses) → 2) sətrin transportAmount.
                // Plan daşınması qaimə yaradılarkən öncədən doldurulur (frontend), ona görə burada
                // plan fallback YOXdur — sənəd cəmi həmişə qaimə cəmi ilə eyni qalsın.
                java.util.Map<Long, java.util.List<com.ces.erp.accounting.entity.InvoiceTransport>> transportsByEq = new java.util.LinkedHashMap<>();
                java.util.List<com.ces.erp.accounting.entity.InvoiceTransport> noEqTransports = new java.util.ArrayList<>();
                for (var t : invTransports) {
                    if (t.getEquipmentId() != null)
                        transportsByEq.computeIfAbsent(t.getEquipmentId(), k -> new java.util.ArrayList<>()).add(t);
                    else noEqTransports.add(t);
                }

                for (var l : invLines) {
                    Long eid = l.getEquipment() != null ? l.getEquipment().getId() : null;
                    var lineTransports = eid != null ? transportsByEq.get(eid) : null;
                    if (lineTransports != null && !lineTransports.isEmpty()) {
                        for (var t : lineTransports) result.add(buildTransportLine(t, eqNames, invId));
                    } else if (l.getTransportAmount() != null && l.getTransportAmount().compareTo(BigDecimal.ZERO) > 0) {
                        result.add(simpleTransportLine(l.getEquipmentName(), l.getTransportAmount(), invId));
                    }
                }
                // equipmentId-siz (köhnə) daşınmalar
                for (var t : noEqTransports) result.add(buildTransportLine(t, eqNames, invId));
            } else {
                // ── Tək-texnikalı (köhnə) qaimə ──
                DocumentLineRequest lineReq = new DocumentLineRequest();
                lineReq.setDescription(buildEquipmentDescription(invoice));
                BigDecimal equipAmount = (invoice.getAmount() != null ? invoice.getAmount() : BigDecimal.ZERO)
                        .subtract(transportTotal);
                lineReq.setUnit("ədəd");
                lineReq.setQuantity(BigDecimal.ONE);
                lineReq.setUnitPrice(equipAmount.compareTo(BigDecimal.ZERO) > 0 ? equipAmount : BigDecimal.ZERO);
                lineReq.setSourceInvoiceId(invId);
                result.add(lineReq);

                // Daşınma sətirləri — qaimədə daxil edilmiş InvoiceTransport qeydlərindən
                if (!invTransports.isEmpty()) {
                    for (var t : invTransports) result.add(buildTransportLine(t, java.util.Map.of(), invId));
                } else {
                    // Fallback: CoordinatorPlan-dan daşınma qiyməti
                    if (invoice.getProject() != null && invoice.getProject().getRequest() != null) {
                        Long requestId = invoice.getProject().getRequest().getId();
                        Optional<CoordinatorPlan> planOpt = coordinatorPlanRepository.findByRequestId(requestId);
                        planOpt.ifPresent(plan -> {
                            if (plan.getTransportationPrice() != null
                                    && plan.getTransportationPrice().compareTo(BigDecimal.ZERO) > 0) {
                                DocumentLineRequest transport = new DocumentLineRequest();
                                String equipName = invoice.getEquipmentName() != null
                                        ? " (" + invoice.getEquipmentName() + ")" : "";
                                transport.setDescription("Daşınma" + equipName);
                                transport.setUnit("dəfə");
                                transport.setQuantity(BigDecimal.ONE);
                                transport.setUnitPrice(plan.getTransportationPrice());
                                transport.setSourceInvoiceId(invId);
                                result.add(transport);
                            }
                        });
                    }
                }
            }
        }

        return result;
    }

    // ─── PDF yüklə ────────────────────────────────────────────────────────────

    public UrlResource downloadPdf(Long id) {
        GeneratedDocument doc = findOrThrow(id);
        if (doc.getPdfFilePath() == null) {
            throw new BusinessException("Bu sənəd üçün PDF mövcud deyil");
        }
        try {
            var path = fileStorageService.resolve(doc.getPdfFilePath());
            return new UrlResource(path.toUri());
        } catch (Exception e) {
            throw new FileStorageException("PDF fayl tapıla bilmədi: " + e.getMessage());
        }
    }

    // ─── PDF yenidən yarat ────────────────────────────────────────────────────

    @Transactional
    public GeneratedDocumentResponse regeneratePdf(Long id) {
        GeneratedDocument doc = findOrThrow(id);

        // Köhnə PDF sil
        if (doc.getPdfFilePath() != null) {
            fileStorageService.delete(doc.getPdfFilePath());
            doc.setPdfFilePath(null);
        }

        // Yeni PDF yarat
        try {
            String pdfPath = pdfGenerationService.generateAndStore(doc);
            doc.setPdfFilePath(pdfPath);
            documentRepository.save(doc);
        } catch (Exception e) {
            throw new FileStorageException("PDF yenidən yaradıla bilmədi: " + e.getMessage());
        }

        auditService.log("SƏNƏD", doc.getId(), doc.getDocumentNumber(),
                "PDF_YENİLƏNDİ", "PDF yenidən yaradıldı");

        return GeneratedDocumentResponse.from(doc);
    }

    // ─── Sil ──────────────────────────────────────────────────────────────────

    @Transactional
    public void delete(Long id) {
        GeneratedDocument doc = findOrThrow(id);

        // PDF sil
        if (doc.getPdfFilePath() != null) {
            fileStorageService.delete(doc.getPdfFilePath());
        }

        auditService.log("SƏNƏD", doc.getId(), doc.getDocumentNumber(),
                "SİLİNDİ", "Sənəd silindi — " + doc.getDocumentType());

        doc.softDelete();
        documentRepository.save(doc);
    }

    // ─── Köməkçi metodlar ─────────────────────────────────────────────────────

    private GeneratedDocument findOrThrow(Long id) {
        return documentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sənəd", id));
    }

    private BigDecimal getVatRate() {
        List<ConfigItemResponse> items = configService.getActiveByCategory("DOCUMENT_VAT_RATE");
        if (items.isEmpty()) return BigDecimal.valueOf(18);
        try {
            return new BigDecimal(items.get(0).getValue());
        } catch (NumberFormatException e) {
            return BigDecimal.valueOf(18);
        }
    }

    /** Toplu qaimə sətrindən (bir texnika) təsvir: "Texnika adı icarəsi (ay/il)". */
    private String buildLineDescription(com.ces.erp.accounting.entity.InvoiceLine line, Invoice invoice) {
        StringBuilder sb = new StringBuilder();
        String name = line.getEquipmentName() != null && !line.getEquipmentName().isBlank()
                ? line.getEquipmentName() : "Texnika";
        sb.append(name).append(" icarəsi");
        Integer pm = line.getPeriodMonth() != null ? line.getPeriodMonth() : invoice.getPeriodMonth();
        Integer py = line.getPeriodYear() != null ? line.getPeriodYear() : invoice.getPeriodYear();
        if (pm != null && py != null) {
            sb.append(" (").append(pm).append("/").append(py).append(")");
        }
        return sb.toString();
    }

    /** Sadə daşınma sətri — texnika adı və məbləğlə (istiqamət olmadan; plan/məbləğ fallback). */
    private DocumentLineRequest simpleTransportLine(String eqName, BigDecimal amount, Long invId) {
        DocumentLineRequest tLine = new DocumentLineRequest();
        String name = (eqName != null && !eqName.isBlank()) ? " (" + eqName + ")" : "";
        tLine.setDescription("Daşınma" + name);
        tLine.setUnit("dəfə");
        tLine.setQuantity(BigDecimal.ONE);
        tLine.setUnitPrice(amount != null ? amount : BigDecimal.ZERO);
        tLine.setSourceInvoiceId(invId);
        return tLine;
    }

    /** Daşınma sətri — texnika adı (varsa) və istiqamətlə. */
    private DocumentLineRequest buildTransportLine(com.ces.erp.accounting.entity.InvoiceTransport t,
                                                   java.util.Map<Long, String> eqNames, Long invId) {
        DocumentLineRequest tLine = new DocumentLineRequest();
        String eqName = (t.getEquipmentId() != null && eqNames.get(t.getEquipmentId()) != null)
                ? " (" + eqNames.get(t.getEquipmentId()) + ")" : "";
        String dir = (t.getTransportDirection() != null && !t.getTransportDirection().isBlank())
                ? " — " + t.getTransportDirection() : "";
        tLine.setDescription("Daşınma" + eqName + dir);
        tLine.setUnit("dəfə");
        tLine.setQuantity(BigDecimal.ONE);
        tLine.setUnitPrice(t.getTransportAmount() != null ? t.getTransportAmount() : BigDecimal.ZERO);
        tLine.setSourceInvoiceId(invId);
        return tLine;
    }

    private String buildEquipmentDescription(Invoice invoice) {
        StringBuilder sb = new StringBuilder();
        if (invoice.getEquipmentName() != null && !invoice.getEquipmentName().isBlank()) {
            sb.append(invoice.getEquipmentName()).append(" icarəsi");
        } else if (invoice.getCompanyName() != null && !invoice.getCompanyName().isBlank()) {
            sb.append(invoice.getCompanyName()).append(" xidməti");
        } else {
            sb.append("Xidmət göstərilməsi");
        }

        // Ay/il əlavə et
        if (invoice.getPeriodMonth() != null && invoice.getPeriodYear() != null) {
            sb.append(" (").append(invoice.getPeriodMonth())
              .append("/").append(invoice.getPeriodYear()).append(")");
        }
        return sb.toString();
    }
}
