package com.ces.erp.accounting.service;

import com.ces.erp.accounting.dto.RequestDocumentCheckResponse;
import com.ces.erp.common.audit.AuditService;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.common.service.FileStorageService;
import com.ces.erp.enums.RequestStatus;
import com.ces.erp.request.entity.RequestDocument;
import com.ces.erp.request.entity.RequestDocumentType;
import com.ces.erp.request.entity.RequestStatusLog;
import com.ces.erp.request.entity.TechRequest;
import com.ces.erp.request.repository.RequestDocumentRepository;
import com.ces.erp.request.repository.RequestStatusLogRepository;
import com.ces.erp.request.repository.TechRequestRepository;
import com.ces.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentCheckService {

    private final TechRequestRepository requestRepository;
    private final RequestDocumentRepository documentRepository;
    private final RequestStatusLogRepository statusLogRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;

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
        if (r.getStatus() != RequestStatus.ACCOUNTING_DOCS_CHECK) {
            throw new BusinessException("Sənəd yükləmək üçün sorğu mühasibatlıq mərhələsində olmalıdır");
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
        fileStorageService.delete(doc.getFilePath());
        doc.softDelete();
        documentRepository.save(doc);
    }

    @Transactional
    public RequestDocumentCheckResponse completeCheck(Long requestId) {
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

        RequestStatus oldStatus = r.getStatus();
        r.setStatus(RequestStatus.EXECUTION_READY);
        requestRepository.save(r);

        String username = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName() : "system";
        statusLogRepository.save(RequestStatusLog.builder()
                .requestId(r.getId())
                .oldStatus(oldStatus)
                .newStatus(RequestStatus.EXECUTION_READY)
                .reason("Sənədlər tamamlandı")
                .changedBy(username)
                .build());
        auditService.log("SORĞU", r.getId(), r.getRequestCode(), "STATUS_DƏYİŞDİ",
                oldStatus.name() + " → EXECUTION_READY | Sənədlər tam");

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
