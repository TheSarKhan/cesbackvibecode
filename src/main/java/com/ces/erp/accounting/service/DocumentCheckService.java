package com.ces.erp.accounting.service;

import com.ces.erp.accounting.dto.RequestDocumentCheckResponse;
import com.ces.erp.accounting.service.ReceivableService;
import com.ces.erp.approval.annotation.RequiresApproval;
import com.ces.erp.approval.context.ApprovalContext;
import com.ces.erp.approval.handler.ApprovalHandler;
import com.ces.erp.common.audit.AuditService;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.common.service.FileStorageService;
import com.ces.erp.enums.ProjectStatus;
import com.ces.erp.enums.RequestStatus;
import com.ces.erp.project.entity.Project;
import com.ces.erp.project.repository.ProjectRepository;
import com.ces.erp.request.entity.RequestDocument;
import com.ces.erp.request.entity.RequestDocumentType;
import com.ces.erp.request.entity.RequestStatusLog;
import com.ces.erp.request.entity.TechRequest;
import com.ces.erp.request.repository.RequestDocumentRepository;
import com.ces.erp.request.repository.RequestStatusLogRepository;
import com.ces.erp.request.repository.TechRequestRepository;
import com.ces.erp.request.service.RequestTransitionService;
import com.ces.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentCheckService implements ApprovalHandler {

    private final TechRequestRepository requestRepository;
    private final RequestDocumentRepository documentRepository;
    private final RequestStatusLogRepository statusLogRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;
    private final RequestTransitionService transitionService;
    private final ProjectRepository projectRepository;
    private final ReceivableService receivableService;

    // ─── Approval handler (PROJECT_ACTIVATION) ───────────────────────────────
    // Mühasibat OK → Əməliyyatların təsdiqi → layihə ACTIVE. Entity = sorğu (requestId).
    @Override public String getEntityType() { return "PROJECT_ACTIVATION"; }
    @Override public String getModuleCode()  { return "ACCOUNTING"; }
    @Override public String getLabel(Long id) { return findOrThrow(id).getRequestCode(); }
    @Override public Object getSnapshot(Long id) {
        return RequestDocumentCheckResponse.from(findOrThrow(id),
                documentRepository.findAllByRequestIdAndDeletedFalse(id));
    }

    @Override
    public void applyEdit(Long id, String json) {
        ApprovalContext.setApplying(true);
        try { applyActivation(id); } finally { ApprovalContext.clear(); }
    }

    @Override public void applyDelete(Long id) { /* istifadə edilmir */ }

    @Transactional(readOnly = true)
    public List<RequestDocumentCheckResponse> getPendingChecks() {
        return requestRepository.findAllByStatusInAndDeletedFalse(
                List.of(RequestStatus.ACCOUNTING_DOCS_CHECK)).stream()
                .map(r -> RequestDocumentCheckResponse.from(r,
                        documentRepository.findAllByRequestIdAndDeletedFalse(r.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public RequestDocumentCheckResponse getCheck(Long requestId) {
        TechRequest r = findOrThrow(requestId);
        return RequestDocumentCheckResponse.from(r,
                documentRepository.findAllByRequestIdAndDeletedFalse(requestId));
    }

    @Transactional
    public RequestDocumentCheckResponse uploadDocument(Long requestId, RequestDocumentType type,
                                                       MultipartFile file, Long userId) {
        TechRequest r = findOrThrow(requestId);
        // PM razılaşma mərhələsindən başlayaraq, mühasibatlıqda da yükləmək mümkündür
        if (r.getStatus() != RequestStatus.PM_PRICE_NEGOTIATION
                && r.getStatus() != RequestStatus.ACCOUNTING_DOCS_CHECK) {
            throw new BusinessException("Sənəd yükləmək üçün sorğu razılaşma və ya mühasibatlıq mərhələsində olmalıdır");
        }

        // Eyni tipdə əvvəlki sənəd varsa onu soft-delete et
        documentRepository.findByRequestIdAndDocTypeAndDeletedFalse(requestId, type).ifPresent(old -> {
            fileStorageService.delete(old.getFilePath());
            old.softDelete();
            documentRepository.save(old);
        });

        String path = fileStorageService.store(file, "request-documents");
        RequestDocument doc = RequestDocument.builder()
                .request(r)
                .docType(type)
                .filePath(path)
                .fileName(file.getOriginalFilename())
                .uploadedBy(userId != null ? userRepository.findById(userId).orElse(null) : null)
                .build();
        documentRepository.save(doc);

        auditService.log("SORĞU", r.getId(), r.getRequestCode(), "SƏNƏD_YÜKLƏNDİ",
                type.name() + " yükləndi: " + file.getOriginalFilename());

        return RequestDocumentCheckResponse.from(r,
                documentRepository.findAllByRequestIdAndDeletedFalse(requestId));
    }

    @Transactional
    public void deleteDocument(Long requestId, Long documentId) {
        RequestDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Sənəd", documentId));
        if (!doc.getRequest().getId().equals(requestId)) {
            throw new BusinessException("Bu sənəd bu sorğuya aid deyil");
        }
        TechRequest r = doc.getRequest();
        if (r.getStatus() != RequestStatus.PM_PRICE_NEGOTIATION
                && r.getStatus() != RequestStatus.ACCOUNTING_DOCS_CHECK) {
            throw new BusinessException("Bu mərhələdə sənəd silinə bilməz");
        }
        fileStorageService.delete(doc.getFilePath());
        doc.softDelete();
        documentRepository.save(doc);
    }

    /**
     * Mühasibat sənəd yoxlamasını "OK" verir → əməliyyat Əməliyyatların təsdiqi modulunda təsdiq
     * tələb edir (PROJECT_ACTIVATION). Təsdiqdən SONRA {@link #applyActivation} işləyir:
     * sorğu EXECUTION_READY + layihə ACTIVE + Receivable.
     * <p>
     * Status + sənəd validasiyası BURADA (submit anında) işləyir — aspect annotasiyalı
     * {@link #submitForActivation}-ı sıraya salmazdan əvvəl. Beləcə əskik sənəd dərhal bloklanır.
     */
    @Transactional
    public RequestDocumentCheckResponse completeCheck(Long requestId) {
        assertReadyForActivation(requestId);
        // Təsdiq qapısı — aspect bunu sıraya salır (PendingApprovalException → 202).
        // Self-invocation AOP-u keçdiyi üçün controller bu metodu BİRBAŞA çağırmalıdır deyil;
        // əvəzinə completeCheck yalnız validasiya edir, controller submitForActivation-ı ayrıca çağırır.
        return RequestDocumentCheckResponse.from(findOrThrow(requestId),
                documentRepository.findAllByRequestIdAndDeletedFalse(requestId));
    }

    /** Status + məcburi sənəd (CONTRACT + PRICE_PROTOCOL) validasiyası — submit anında. */
    @Transactional(readOnly = true)
    public void assertReadyForActivation(Long requestId) {
        TechRequest r = findOrThrow(requestId);
        if (r.getStatus() != RequestStatus.ACCOUNTING_DOCS_CHECK) {
            throw new BusinessException("Sənəd yoxlaması yalnız ACCOUNTING_DOCS_CHECK statusunda tamamlana bilər");
        }
        boolean hasContract = documentRepository.existsByRequestIdAndDocTypeAndDeletedFalse(
                requestId, RequestDocumentType.CONTRACT);
        boolean hasProtocol = documentRepository.existsByRequestIdAndDocTypeAndDeletedFalse(
                requestId, RequestDocumentType.PRICE_PROTOCOL);
        if (!hasContract || !hasProtocol) {
            throw new BusinessException("Müqavilə və qiymət razılaşma protokolu yüklənməlidir");
        }
    }

    /**
     * Təsdiq qapısı — controller BİRBAŞA çağırır (proxy sərhədi keçsin deyə).
     * Aspect ApprovalContext.isApplying() olmayanda bu metodu icra ETMİR — PendingOperation
     * yaradıb {@code PROJECT_ACTIVATION} sıraya salır və 202 qaytarır. Təsdiqdə applyEdit→applyActivation.
     */
    @Transactional
    @RequiresApproval(module = "ACCOUNTING", entityType = "PROJECT_ACTIVATION")
    public RequestDocumentCheckResponse submitForActivation(Long requestId) {
        // Yalnız apply-proceed olduqda işləyər (normalda aspect sıraya salır).
        applyActivation(requestId);
        return RequestDocumentCheckResponse.from(findOrThrow(requestId),
                documentRepository.findAllByRequestIdAndDeletedFalse(requestId));
    }

    /**
     * Təsdiqlənmiş aktivləşmə effekti: sorğu EXECUTION_READY + layihə (PENDING→ACTIVE) + Receivable.
     * Sənəd validasiyası təsdiqdə də təkrarlanır (submit ilə təsdiq arasında sənəd silinə bilərdi).
     */
    private void applyActivation(Long requestId) {
        TechRequest r = findOrThrow(requestId);
        assertReadyForActivation(requestId);

        // Mərkəzi gateway: ACCOUNTING_DOCS_CHECK → EXECUTION_READY (validasiya + log + audit)
        transitionService.transition(r, RequestStatus.EXECUTION_READY,
                "Mühasibat təsdiqi — layihə aktivləşdirildi", null);

        // Layihə (PENDING) → ACTIVE + başlanğıc tarixi + Receivable (yeganə aktivləşmə nöqtəsi)
        projectRepository.findByRequestIdAndDeletedFalse(requestId).ifPresent(p -> {
            if (p.getStatus() == ProjectStatus.PENDING) {
                p.setStatus(ProjectStatus.ACTIVE);
                if (p.getStartDate() == null) p.setStartDate(LocalDate.now());
                projectRepository.save(p);
                receivableService.createFromProject(p);
            }
        });
    }

    /**
     * Geri qaytarma — maliyyə sənəd əskik/səhv olduqda sorğunu LM-ə (qiymət danışığına) qaytarır.
     * ACCOUNTING_DOCS_CHECK → PM_PRICE_NEGOTIATION. Səbəb məcburi.
     * PM_APPROVED-da yaranmış PENDING Project SAXLANILIR (silinmir) — LM yenidən təsdiqləyəndə dublikat yaranmır.
     */
    @Transactional
    public RequestDocumentCheckResponse sendBackToProjectManager(Long requestId, String reason) {
        TechRequest r = findOrThrow(requestId);
        if (r.getStatus() != RequestStatus.ACCOUNTING_DOCS_CHECK) {
            throw new BusinessException("Geri qaytarma yalnız ACCOUNTING_DOCS_CHECK statusunda mümkündür");
        }
        transitionService.transition(r, RequestStatus.PM_PRICE_NEGOTIATION, reason, null);
        return RequestDocumentCheckResponse.from(r,
                documentRepository.findAllByRequestIdAndDeletedFalse(requestId));
    }

    public Path resolveDocumentFile(Long requestId, Long documentId) {
        RequestDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Sənəd", documentId));
        if (!doc.getRequest().getId().equals(requestId)) {
            throw new BusinessException("Bu sənəd bu sorğuya aid deyil");
        }
        return fileStorageService.resolve(doc.getFilePath());
    }

    private TechRequest findOrThrow(Long requestId) {
        return requestRepository.findByIdAndDeletedFalse(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Sorğu", requestId));
    }
}
