package com.ces.erp.partydoc;

import com.ces.erp.accounting.entity.GeneratedDocument;
import com.ces.erp.accounting.entity.Invoice;
import com.ces.erp.accounting.repository.GeneratedDocumentRepository;
import com.ces.erp.accounting.repository.InvoiceRepository;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.common.service.FileStorageService;
import com.ces.erp.coordinator.entity.CoordinatorDocument;
import com.ces.erp.coordinator.repository.CoordinatorDocumentRepository;
import com.ces.erp.customer.entity.Customer;
import com.ces.erp.customer.entity.CustomerDocument;
import com.ces.erp.customer.repository.CustomerDocumentRepository;
import com.ces.erp.customer.repository.CustomerRepository;
import com.ces.erp.contractor.entity.Contractor;
import com.ces.erp.contractor.entity.ContractorDocument;
import com.ces.erp.contractor.repository.ContractorDocumentRepository;
import com.ces.erp.contractor.repository.ContractorRepository;
import com.ces.erp.garage.entity.EquipmentDocument;
import com.ces.erp.garage.repository.EquipmentDocumentRepository;
import com.ces.erp.investor.entity.Investor;
import com.ces.erp.investor.entity.InvestorDocument;
import com.ces.erp.investor.repository.InvestorDocumentRepository;
import com.ces.erp.investor.repository.InvestorRepository;
import com.ces.erp.request.entity.RequestDocument;
import com.ces.erp.request.entity.RequestDocumentType;
import com.ces.erp.request.repository.RequestDocumentRepository;
import com.ces.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Sənəd mərkəzi — bir tərəfin (müştəri/podratçı/investor) BÜTÜN sənədlərini
 * müxtəlif mənbələrdən toplayır: əl ilə yüklənənlər, müqavilə/protokollar,
 * təhvil-təslim aktları, texnika qaraj sənədləri və qaimə/fakturalar.
 */
@Service
@RequiredArgsConstructor
public class PartyDocumentService {

    private static final String HANDOVER_ACT = "HANDOVER_ACT";
    private static final List<RequestDocumentType> CUSTOMER_TYPES =
            List.of(RequestDocumentType.CONTRACT, RequestDocumentType.PRICE_PROTOCOL);
    private static final List<RequestDocumentType> OWNER_TYPES =
            List.of(RequestDocumentType.OWNER_CONTRACT, RequestDocumentType.OWNER_PRICE_PROTOCOL);

    private final CustomerDocumentRepository customerDocumentRepository;
    private final ContractorDocumentRepository contractorDocumentRepository;
    private final InvestorDocumentRepository investorDocumentRepository;
    private final RequestDocumentRepository requestDocumentRepository;
    private final CoordinatorDocumentRepository coordinatorDocumentRepository;
    private final EquipmentDocumentRepository equipmentDocumentRepository;
    private final GeneratedDocumentRepository generatedDocumentRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvestorRepository investorRepository;
    private final CustomerRepository customerRepository;
    private final ContractorRepository contractorRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    // ─── Aqreqasiya ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PartyDocumentDto> collect(PartyKind kind, Long partyId) {
        return switch (kind) {
            case CUSTOMER -> collectCustomer(partyId);
            case CONTRACTOR -> collectContractor(partyId);
            case INVESTOR -> collectInvestor(partyId);
        };
    }

    private List<PartyDocumentDto> collectCustomer(Long id) {
        List<PartyDocumentDto> out = new ArrayList<>();
        // Əl ilə
        for (CustomerDocument d : customerDocumentRepository.findAllByCustomerIdAndDeletedFalseOrderByCreatedAtDesc(id)) {
            out.add(PartyDocumentDto.builder()
                    .category("Əl ilə yüklənən").sourceType("MANUAL").sourceId(d.getId())
                    .name(nz(d.getDocumentName())).fileType(d.getFileType())
                    .date(d.getDocumentDate() != null ? d.getDocumentDate() : toDate(d.getCreatedAt()))
                    .manual(true).build());
        }
        // Müqavilə + protokol (müştəri tərəfi)
        for (RequestDocument d : requestDocumentRepository.findCustomerSideDocs(id, CUSTOMER_TYPES)) {
            out.add(requestDocDto(d));
        }
        // Təhvil-təslim aktları
        for (CoordinatorDocument d : coordinatorDocumentRepository.findByCustomerAndType(id, HANDOVER_ACT)) {
            out.add(handoverDto(d));
        }
        // Qaimələr / fakturalar (yaradılmış PDF)
        for (GeneratedDocument d : generatedDocumentRepository.findAllByCustomerIdAndDeletedFalseOrderByCreatedAtDesc(id)) {
            if (d.getPdfFilePath() == null) continue;
            out.add(generatedDto(d));
        }
        // Qaimə aktları (gəlir qaimələrinə əlavə akt faylı)
        for (Invoice i : invoiceRepository.findAllByCustomerId(id)) {
            if (i.getAktFilePath() != null) out.add(invoiceAktDto(i));
        }
        return out;
    }

    private List<PartyDocumentDto> collectContractor(Long id) {
        List<PartyDocumentDto> out = new ArrayList<>();
        for (ContractorDocument d : contractorDocumentRepository.findAllByContractorIdAndDeletedFalseOrderByCreatedAtDesc(id)) {
            out.add(PartyDocumentDto.builder()
                    .category("Əl ilə yüklənən").sourceType("MANUAL").sourceId(d.getId())
                    .name(nz(d.getDocumentName())).fileType(d.getFileType())
                    .date(d.getDocumentDate() != null ? d.getDocumentDate() : toDate(d.getCreatedAt()))
                    .manual(true).build());
        }
        for (RequestDocument d : requestDocumentRepository.findContractorSideDocs(id, OWNER_TYPES)) {
            out.add(requestDocDto(d));
        }
        for (CoordinatorDocument d : coordinatorDocumentRepository.findByContractorAndType(id, HANDOVER_ACT)) {
            out.add(handoverDto(d));
        }
        for (EquipmentDocument d : equipmentDocumentRepository.findByOwnerContractor(id)) {
            out.add(equipmentDocDto(d));
        }
        for (Invoice i : invoiceRepository.findAllByContractorId(id)) {
            if (i.getAktFilePath() != null) out.add(invoiceAktDto(i));
        }
        return out;
    }

    private List<PartyDocumentDto> collectInvestor(Long id) {
        List<PartyDocumentDto> out = new ArrayList<>();
        for (InvestorDocument d : investorDocumentRepository.findAllByInvestorIdAndDeletedFalseOrderByCreatedAtDesc(id)) {
            out.add(PartyDocumentDto.builder()
                    .category("Əl ilə yüklənən").sourceType("MANUAL").sourceId(d.getId())
                    .name(nz(d.getDocumentName())).fileType(d.getFileType())
                    .date(d.getDocumentDate() != null ? d.getDocumentDate() : toDate(d.getCreatedAt()))
                    .manual(true).build());
        }
        for (RequestDocument d : requestDocumentRepository.findInvestorSideDocs(id, OWNER_TYPES)) {
            out.add(requestDocDto(d));
        }
        for (CoordinatorDocument d : coordinatorDocumentRepository.findByInvestorAndType(id, HANDOVER_ACT)) {
            out.add(handoverDto(d));
        }
        Investor inv = investorRepository.findById(id).orElse(null);
        if (inv != null && inv.getVoen() != null) {
            for (EquipmentDocument d : equipmentDocumentRepository.findByOwnerInvestorVoen(inv.getVoen())) {
                out.add(equipmentDocDto(d));
            }
        }
        for (Invoice i : invoiceRepository.findAllByInvestorId(id)) {
            if (i.getAktFilePath() != null) out.add(invoiceAktDto(i));
        }
        return out;
    }

    // ─── Endirmə (mənbə tipinə görə, sahiblik yoxlaması ilə) ──────────────────

    @Transactional(readOnly = true)
    public DownloadFile resolveDownload(PartyKind kind, Long partyId, String sourceType, Long sourceId) {
        return switch (sourceType) {
            case "MANUAL" -> resolveManual(kind, partyId, sourceId);
            case "REQUEST_DOC" -> {
                RequestDocument d = requestDocumentRepository.findById(sourceId)
                        .filter(x -> !x.isDeleted() && requestDocBelongs(x, kind, partyId))
                        .orElseThrow(() -> notFound(sourceId));
                yield new DownloadFile(fileStorageService.resolve(d.getFilePath()), d.getFileName());
            }
            case "COORDINATOR_DOC" -> {
                CoordinatorDocument d = coordinatorDocumentRepository.findById(sourceId)
                        .filter(x -> !x.isDeleted() && coordinatorDocBelongs(x, kind, partyId))
                        .orElseThrow(() -> notFound(sourceId));
                yield new DownloadFile(fileStorageService.resolve(d.getFilePath()), d.getDocumentName());
            }
            case "EQUIPMENT_DOC" -> {
                EquipmentDocument d = equipmentDocumentRepository.findById(sourceId)
                        .filter(x -> equipmentDocBelongs(x, kind, partyId))
                        .orElseThrow(() -> notFound(sourceId));
                yield new DownloadFile(fileStorageService.resolve(d.getFilePath()), d.getDocumentName());
            }
            case "GENERATED_DOC" -> {
                GeneratedDocument d = generatedDocumentRepository.findByIdAndDeletedFalse(sourceId)
                        .filter(x -> kind == PartyKind.CUSTOMER && x.getCustomer() != null
                                && x.getCustomer().getId().equals(partyId) && x.getPdfFilePath() != null)
                        .orElseThrow(() -> notFound(sourceId));
                yield new DownloadFile(fileStorageService.resolve(d.getPdfFilePath()),
                        "Sənəd-" + d.getDocumentNumber() + ".pdf");
            }
            case "INVOICE_AKT" -> {
                Invoice i = invoiceRepository.findById(sourceId)
                        .filter(x -> !x.isDeleted() && x.getAktFilePath() != null && invoiceBelongs(x, kind, partyId))
                        .orElseThrow(() -> notFound(sourceId));
                yield new DownloadFile(fileStorageService.resolve(i.getAktFilePath()),
                        nz(i.getAktFileName()));
            }
            default -> throw notFound(sourceId);
        };
    }

    private DownloadFile resolveManual(PartyKind kind, Long partyId, Long sourceId) {
        return switch (kind) {
            case CUSTOMER -> {
                CustomerDocument d = customerDocumentRepository.findByIdAndCustomerId(sourceId, partyId)
                        .orElseThrow(() -> notFound(sourceId));
                yield new DownloadFile(fileStorageService.resolve(d.getFilePath()), d.getDocumentName());
            }
            case CONTRACTOR -> {
                ContractorDocument d = contractorDocumentRepository.findByIdAndContractorId(sourceId, partyId)
                        .orElseThrow(() -> notFound(sourceId));
                yield new DownloadFile(fileStorageService.resolve(d.getFilePath()), d.getDocumentName());
            }
            case INVESTOR -> {
                InvestorDocument d = investorDocumentRepository.findByIdAndInvestorId(sourceId, partyId)
                        .orElseThrow(() -> notFound(sourceId));
                yield new DownloadFile(fileStorageService.resolve(d.getFilePath()), d.getDocumentName());
            }
        };
    }

    // ─── Əl ilə sənəd yükləmə / silmə ────────────────────────────────────────

    @Transactional
    public PartyDocumentDto uploadManual(PartyKind kind, Long partyId, MultipartFile file,
                                         String documentName, String documentDate, Long userId) {
        String subDir = switch (kind) {
            case CUSTOMER -> "customer-documents";
            case CONTRACTOR -> "contractor-documents";
            case INVESTOR -> "investor-documents";
        };
        String path = fileStorageService.store(file, subDir);
        String originalName = file.getOriginalFilename();
        String fileType = ext(originalName);
        String name = documentName != null && !documentName.isBlank() ? documentName : originalName;
        LocalDate date = (documentDate != null && !documentDate.isBlank())
                ? LocalDate.parse(documentDate) : LocalDate.now();
        var user = userId != null ? userRepository.findById(userId).orElse(null) : null;

        Long savedId;
        switch (kind) {
            case CUSTOMER -> {
                Customer c = customerRepository.findById(partyId)
                        .orElseThrow(() -> new ResourceNotFoundException("Müştəri", partyId));
                CustomerDocument d = customerDocumentRepository.save(CustomerDocument.builder()
                        .customer(c).filePath(path).documentName(name).fileType(fileType)
                        .documentDate(date).uploadedBy(user).build());
                savedId = d.getId();
            }
            case CONTRACTOR -> {
                Contractor c = contractorRepository.findById(partyId)
                        .orElseThrow(() -> new ResourceNotFoundException("Podratçı", partyId));
                ContractorDocument d = contractorDocumentRepository.save(ContractorDocument.builder()
                        .contractor(c).filePath(path).documentName(name).fileType(fileType)
                        .documentDate(date).uploadedBy(user).build());
                savedId = d.getId();
            }
            case INVESTOR -> {
                Investor inv = investorRepository.findById(partyId)
                        .orElseThrow(() -> new ResourceNotFoundException("İnvestor", partyId));
                InvestorDocument d = investorDocumentRepository.save(InvestorDocument.builder()
                        .investor(inv).filePath(path).documentName(name).fileType(fileType)
                        .documentDate(date).uploadedBy(user).build());
                savedId = d.getId();
            }
            default -> throw notFound(partyId);
        }
        return PartyDocumentDto.builder()
                .category("Əl ilə yüklənən").sourceType("MANUAL").sourceId(savedId)
                .name(nz(name)).fileType(fileType).date(date).manual(true).build();
    }

    @Transactional
    public void deleteManual(PartyKind kind, Long partyId, Long documentId) {
        switch (kind) {
            case CUSTOMER -> {
                CustomerDocument d = customerDocumentRepository.findByIdAndCustomerId(documentId, partyId)
                        .orElseThrow(() -> notFound(documentId));
                fileStorageService.delete(d.getFilePath());
                d.softDelete();
                customerDocumentRepository.save(d);
            }
            case CONTRACTOR -> {
                ContractorDocument d = contractorDocumentRepository.findByIdAndContractorId(documentId, partyId)
                        .orElseThrow(() -> notFound(documentId));
                fileStorageService.delete(d.getFilePath());
                d.softDelete();
                contractorDocumentRepository.save(d);
            }
            case INVESTOR -> {
                InvestorDocument d = investorDocumentRepository.findByIdAndInvestorId(documentId, partyId)
                        .orElseThrow(() -> notFound(documentId));
                fileStorageService.delete(d.getFilePath());
                d.softDelete();
                investorDocumentRepository.save(d);
            }
        }
    }

    // ─── Sahiblik yoxlamaları ────────────────────────────────────────────────

    private boolean requestDocBelongs(RequestDocument d, PartyKind kind, Long partyId) {
        return switch (kind) {
            case CUSTOMER -> d.getRequest() != null && d.getRequest().getCustomer() != null
                    && d.getRequest().getCustomer().getId().equals(partyId)
                    && CUSTOMER_TYPES.contains(d.getDocType());
            case CONTRACTOR -> d.getPlanItem() != null && d.getPlanItem().getContractor() != null
                    && d.getPlanItem().getContractor().getId().equals(partyId)
                    && OWNER_TYPES.contains(d.getDocType());
            case INVESTOR -> d.getPlanItem() != null && d.getPlanItem().getInvestor() != null
                    && d.getPlanItem().getInvestor().getId().equals(partyId)
                    && OWNER_TYPES.contains(d.getDocType());
        };
    }

    private boolean coordinatorDocBelongs(CoordinatorDocument d, PartyKind kind, Long partyId) {
        return switch (kind) {
            case CUSTOMER -> d.getPlan() != null && d.getPlan().getRequest() != null
                    && d.getPlan().getRequest().getCustomer() != null
                    && d.getPlan().getRequest().getCustomer().getId().equals(partyId);
            case CONTRACTOR -> d.getPlanItem() != null && d.getPlanItem().getContractor() != null
                    && d.getPlanItem().getContractor().getId().equals(partyId);
            case INVESTOR -> d.getPlanItem() != null && d.getPlanItem().getInvestor() != null
                    && d.getPlanItem().getInvestor().getId().equals(partyId);
        };
    }

    private boolean equipmentDocBelongs(EquipmentDocument d, PartyKind kind, Long partyId) {
        if (d.getEquipment() == null) return false;
        return switch (kind) {
            case CONTRACTOR -> d.getEquipment().getOwnerContractor() != null
                    && d.getEquipment().getOwnerContractor().getId().equals(partyId);
            case INVESTOR -> {
                Investor inv = investorRepository.findById(partyId).orElse(null);
                yield inv != null && inv.getVoen() != null
                        && inv.getVoen().equals(d.getEquipment().getOwnerInvestorVoen());
            }
            case CUSTOMER -> false;
        };
    }

    private boolean invoiceBelongs(Invoice i, PartyKind kind, Long partyId) {
        return switch (kind) {
            case CUSTOMER -> i.getCustomer() != null && i.getCustomer().getId().equals(partyId);
            case CONTRACTOR -> i.getContractor() != null && i.getContractor().getId().equals(partyId);
            case INVESTOR -> i.getInvestor() != null && i.getInvestor().getId().equals(partyId);
        };
    }

    // ─── DTO qurucular ───────────────────────────────────────────────────────

    private PartyDocumentDto requestDocDto(RequestDocument d) {
        String project = d.getRequest() != null ? d.getRequest().getProjectName() : null;
        String eq = d.getPlanItem() != null && d.getPlanItem().getEquipment() != null
                ? d.getPlanItem().getEquipment().getName() : null;
        return PartyDocumentDto.builder()
                .category("Müqavilələr").sourceType("REQUEST_DOC").sourceId(d.getId())
                .name(d.getDocType() != null ? d.getDocType().getLabel() : nz(d.getFileName()))
                .fileType(ext(d.getFileName()))
                .context(joinCtx(project, eq))
                .date(toDate(d.getCreatedAt())).manual(false).build();
    }

    private PartyDocumentDto handoverDto(CoordinatorDocument d) {
        String project = d.getPlan() != null && d.getPlan().getRequest() != null
                ? d.getPlan().getRequest().getProjectName() : null;
        String eq = d.getPlanItem() != null && d.getPlanItem().getEquipment() != null
                ? d.getPlanItem().getEquipment().getName() : null;
        return PartyDocumentDto.builder()
                .category("Təhvil-təslim aktları").sourceType("COORDINATOR_DOC").sourceId(d.getId())
                .name(d.getDocumentName() != null ? d.getDocumentName() : "Təhvil-təslim aktı")
                .fileType(nz2(d.getFileType(), ext(d.getDocumentName())))
                .context(joinCtx(project, eq))
                .date(toDate(d.getCreatedAt())).manual(false).build();
    }

    private PartyDocumentDto equipmentDocDto(EquipmentDocument d) {
        String eq = d.getEquipment() != null ? d.getEquipment().getName() : null;
        return PartyDocumentDto.builder()
                .category("Texnika sənədləri").sourceType("EQUIPMENT_DOC").sourceId(d.getId())
                .name(d.getDocumentName() != null ? d.getDocumentName() : nz(d.getDocumentType()))
                .fileType(nz2(d.getFileType(), ext(d.getDocumentName())))
                .context(eq)
                .date(toDate(d.getCreatedAt())).manual(false).build();
    }

    private PartyDocumentDto generatedDto(GeneratedDocument d) {
        return PartyDocumentDto.builder()
                .category("Qaimələr / Fakturalar").sourceType("GENERATED_DOC").sourceId(d.getId())
                .name((d.getDocumentType() != null ? d.getDocumentType().getLabel() : "Sənəd")
                        + " №" + d.getDocumentNumber())
                .fileType("PDF")
                .date(d.getDocumentDate())
                .manual(false).build();
    }

    private PartyDocumentDto invoiceAktDto(Invoice i) {
        String project = i.getProject() != null ? i.getProject().getProjectCode() : null;
        return PartyDocumentDto.builder()
                .category("Qaimə aktları").sourceType("INVOICE_AKT").sourceId(i.getId())
                .name("Akt — " + (i.getInvoiceNumber() != null ? i.getInvoiceNumber()
                        : (i.getAccountingId() != null ? i.getAccountingId() : ("#" + i.getId()))))
                .fileType(ext(i.getAktFileName()))
                .context(joinCtx(project, i.getEquipmentName()))
                .date(i.getInvoiceDate()).manual(false).build();
    }

    // ─── Yardımçı ────────────────────────────────────────────────────────────

    private static ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("Sənəd", id);
    }

    private static LocalDate toDate(LocalDateTime dt) {
        return dt != null ? dt.toLocalDate() : null;
    }

    private static String nz(String s) {
        return s != null && !s.isBlank() ? s : "Sənəd";
    }

    private static String nz2(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }

    private static String ext(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "FILE";
        return fileName.substring(fileName.lastIndexOf('.') + 1).toUpperCase();
    }

    private static String joinCtx(String a, String b) {
        if (a != null && b != null) return a + " · " + b;
        return a != null ? a : b;
    }

    /** Endirmə üçün fayl yolu + təklif olunan ad. */
    public record DownloadFile(Path path, String fileName) {}
}
