package com.ces.erp.operator.service;

import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.common.service.FileStorageService;
import com.ces.erp.enums.OperatorDocumentType;
import com.ces.erp.operator.dto.OperatorRequest;
import com.ces.erp.operator.dto.OperatorResponse;
import com.ces.erp.operator.entity.Operator;
import com.ces.erp.operator.entity.OperatorDocument;
import com.ces.erp.operator.repository.OperatorDocumentRepository;
import com.ces.erp.operator.repository.OperatorRepository;
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
public class OperatorService {

    private final OperatorRepository operatorRepository;
    private final OperatorDocumentRepository documentRepository;
    private final FileStorageService fileStorageService;

    public List<OperatorResponse> getAll() {
        return operatorRepository.findAllActive().stream()
                .map(OperatorResponse::from)
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
        return OperatorResponse.from(operatorRepository.save(o));
    }

    @Transactional
    public OperatorResponse update(Long id, OperatorRequest req) {
        Operator o = findOrThrow(id);
        o.setFirstName(req.getFirstName());
        o.setLastName(req.getLastName());
        o.setAddress(req.getAddress());
        o.setPhone(req.getPhone());
        o.setEmail(req.getEmail());
        o.setSpecialization(req.getSpecialization());
        o.setNotes(req.getNotes());
        return OperatorResponse.from(operatorRepository.save(o));
    }

    @Transactional
    public void delete(Long id) {
        Operator o = findOrThrow(id);
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
