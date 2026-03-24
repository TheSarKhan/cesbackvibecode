package com.ces.erp.operator.service;

import com.ces.erp.approval.annotation.RequiresApproval;
import com.ces.erp.approval.context.ApprovalContext;
import com.ces.erp.approval.handler.ApprovalHandler;
import com.ces.erp.common.audit.AuditService;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.common.service.FileStorageService;
import com.ces.erp.enums.OperatorDocumentType;
import com.ces.erp.operator.dto.OperatorRequest;
import com.ces.erp.operator.dto.OperatorResponse;
import com.ces.erp.operator.entity.Operator;
import com.ces.erp.operator.entity.OperatorDocument;
import com.ces.erp.operator.repository.OperatorDocumentRepository;
import com.ces.erp.operator.repository.OperatorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OperatorService implements ApprovalHandler {

    private final OperatorRepository operatorRepository;
    private final OperatorDocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    @Override public String getEntityType() { return "OPERATOR"; }
    @Override public String getModuleCode()  { return "OPERATORS"; }
    @Override public String getLabel(Long id) {
        Operator o = findOrThrow(id);
        return o.getFirstName() + " " + o.getLastName();
    }
    @Override public Object getSnapshot(Long id) { return OperatorResponse.from(findOrThrow(id)); }

    @Override
    public void applyEdit(Long id, String json) {
        try {
            OperatorRequest req = objectMapper.readValue(json, OperatorRequest.class);
            ApprovalContext.setApplying(true);
            try { update(id, req); } finally { ApprovalContext.clear(); }
        } catch (Exception e) { throw new RuntimeException("applyEdit xətası: " + e.getMessage(), e); }
    }

    @Override
    public void applyDelete(Long id) {
        ApprovalContext.setApplying(true);
        try { delete(id); } finally { ApprovalContext.clear(); }
    }

    public List<OperatorResponse> getAll() {
        var busyIds = operatorRepository.findBusyOperatorIds();
        return operatorRepository.findAllActive().stream()
                .map(o -> {
                    OperatorResponse r = OperatorResponse.from(o);
                    r.setBusy(busyIds.contains(o.getId()));
                    return r;
                })
                .toList();
    }

    public OperatorResponse getById(Long id) {
        return OperatorResponse.from(findOrThrow(id));
    }

    @Transactional
    public OperatorResponse create(OperatorRequest req) {
        Operator o = Operator.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .address(req.getAddress())
                .phone(req.getPhone())
                .email(req.getEmail())
                .specialization(req.getSpecialization())
                .notes(req.getNotes())
                .build();
        Operator saved = operatorRepository.save(o);
        auditService.log("OPERATOR", saved.getId(), saved.getFirstName() + " " + saved.getLastName(), "YARADILDI", "Yeni operator qeydiyyatı");
        return OperatorResponse.from(saved);
    }

    @Transactional
    @RequiresApproval(module = "OPERATORS", entityType = "OPERATOR")
    public OperatorResponse update(Long id, OperatorRequest req) {
        Operator o = findOrThrow(id);
        o.setFirstName(req.getFirstName());
        o.setLastName(req.getLastName());
        o.setAddress(req.getAddress());
        o.setPhone(req.getPhone());
        o.setEmail(req.getEmail());
        o.setSpecialization(req.getSpecialization());
        o.setNotes(req.getNotes());
        Operator updated = operatorRepository.save(o);
        auditService.log("OPERATOR", updated.getId(), updated.getFirstName() + " " + updated.getLastName(), "YENİLƏNDİ", "Operator məlumatları yeniləndi");
        return OperatorResponse.from(updated);
    }

    @Transactional
    @RequiresApproval(module = "OPERATORS", entityType = "OPERATOR", isDelete = true)
    public void delete(Long id) {
        Operator o = findOrThrow(id);
        auditService.log("OPERATOR", o.getId(), o.getFirstName() + " " + o.getLastName(), "SİLİNDİ", "Operator silindi");
        o.softDelete();
        operatorRepository.save(o);
    }

    // ─── Sənəd yüklə / sil ───────────────────────────────────────────────────

    @Transactional
    public OperatorResponse uploadDocument(Long id, OperatorDocumentType type, MultipartFile file) {
        Operator o = findOrThrow(id);

        // Əgər bu tip artıq varsa, əvvəlki faylı sil
        documentRepository.findByOperatorIdAndDocumentType(id, type).ifPresent(existing -> {
            fileStorageService.delete(existing.getFilePath());
            documentRepository.delete(existing);
        });

        String path = fileStorageService.store(file, "operator-documents");
        OperatorDocument doc = OperatorDocument.builder()
                .operator(o)
                .documentType(type)
                .filePath(path)
                .fileName(file.getOriginalFilename())
                .build();
        documentRepository.save(doc);

        return OperatorResponse.from(findOrThrow(id));
    }

    @Transactional
    public OperatorResponse deleteDocument(Long id, Long docId) {
        findOrThrow(id);
        OperatorDocument doc = documentRepository.findByIdAndOperatorId(docId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Sənəd", docId));
        fileStorageService.delete(doc.getFilePath());
        documentRepository.delete(doc);
        return OperatorResponse.from(findOrThrow(id));
    }

    public Path resolveDocumentPath(Long id, Long docId) {
        findOrThrow(id);
        OperatorDocument doc = documentRepository.findByIdAndOperatorId(docId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Sənəd", docId));
        return fileStorageService.resolve(doc.getFilePath());
    }

    // ─── Yardımçı ────────────────────────────────────────────────────────────

    private Operator findOrThrow(Long id) {
        return operatorRepository.findByIdActive(id)
                .orElseThrow(() -> new ResourceNotFoundException("Operator", id));
    }
}
