package com.ces.erp.customer.service;

import com.ces.erp.approval.annotation.RequiresApproval;
import com.ces.erp.approval.context.ApprovalContext;
import com.ces.erp.approval.handler.ApprovalHandler;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.common.service.FileStorageService;
import com.ces.erp.customer.dto.CustomerDocumentResponse;
import com.ces.erp.customer.dto.CustomerRequest;
import com.ces.erp.customer.dto.CustomerResponse;
import com.ces.erp.customer.entity.Customer;
import com.ces.erp.customer.entity.CustomerDocument;
import com.ces.erp.customer.repository.CustomerDocumentRepository;
import com.ces.erp.customer.repository.CustomerRepository;
import com.ces.erp.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService implements ApprovalHandler {

    private final CustomerRepository customerRepository;
    private final CustomerDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    @Override public String getEntityType() { return "CUSTOMER"; }
    @Override public String getModuleCode()  { return "CUSTOMER_MANAGEMENT"; }
    @Override public String getLabel(Long id) { return findOrThrow(id).getCompanyName(); }
    @Override public Object getSnapshot(Long id) { return CustomerResponse.from(findOrThrow(id)); }

    @Override
    public void applyEdit(Long id, String json) {
        try {
            CustomerRequest req = objectMapper.readValue(json, CustomerRequest.class);
            ApprovalContext.setApplying(true);
            try { update(id, req); } finally { ApprovalContext.clear(); }
        } catch (Exception e) { throw new RuntimeException("applyEdit xətası: " + e.getMessage(), e); }
    }

    @Override
    public void applyDelete(Long id) {
        ApprovalContext.setApplying(true);
        try { delete(id); } finally { ApprovalContext.clear(); }
    }

    public List<CustomerResponse> getAll() {
        return customerRepository.findAllByDeletedFalse().stream()
                .map(CustomerResponse::from)
                .toList();
    }

    public CustomerResponse getById(Long id) {
        return CustomerResponse.from(findOrThrow(id));
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        if (request.getVoen() != null && !request.getVoen().isBlank()
                && customerRepository.existsByVoenAndDeletedFalse(request.getVoen())) {
            throw new BusinessException("Bu VÖEN artıq qeydiyyatdadır");
        }
        return CustomerResponse.from(customerRepository.save(toEntity(request, new Customer())));
    }

    @Transactional
    @RequiresApproval(module = "CUSTOMER_MANAGEMENT", entityType = "CUSTOMER")
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = findOrThrow(id);
        if (request.getVoen() != null && !request.getVoen().isBlank()
                && customerRepository.existsByVoenAndIdNotAndDeletedFalse(request.getVoen(), id)) {
            throw new BusinessException("Bu VÖEN artıq qeydiyyatdadır");
        }
        return CustomerResponse.from(customerRepository.save(toEntity(request, customer)));
    }

    @Transactional
    @RequiresApproval(module = "CUSTOMER_MANAGEMENT", entityType = "CUSTOMER", isDelete = true)
    public void delete(Long id) {
        Customer customer = findOrThrow(id);
        customer.softDelete();
        customerRepository.save(customer);
    }

    // ─── Sənədlər ─────────────────────────────────────────────────────────────

    @Transactional
    public CustomerDocumentResponse uploadDocument(Long customerId, MultipartFile file,
                                                    String documentName, Long uploadedByUserId) {
        Customer customer = findOrThrow(customerId);
        String path = fileStorageService.store(file, "customer-documents");
        String originalName = file.getOriginalFilename();
        String fileType = originalName != null && originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf(".") + 1).toUpperCase()
                : "FILE";

        CustomerDocument doc = CustomerDocument.builder()
                .customer(customer)
                .filePath(path)
                .documentName(documentName != null && !documentName.isBlank() ? documentName : originalName)
                .fileType(fileType)
                .uploadedBy(uploadedByUserId != null
                        ? userRepository.findById(uploadedByUserId).orElse(null)
                        : null)
                .build();

        return CustomerDocumentResponse.from(documentRepository.save(doc));
    }

    @Transactional
    public void deleteDocument(Long customerId, Long documentId) {
        CustomerDocument doc = documentRepository.findByIdAndCustomerId(documentId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Sənəd", documentId));
        fileStorageService.delete(doc.getFilePath());
        doc.softDelete();
        documentRepository.save(doc);
    }

    public Path resolveDocumentPath(Long customerId, Long documentId) {
        CustomerDocument doc = documentRepository.findByIdAndCustomerId(documentId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Sənəd", documentId));
        return fileStorageService.resolve(doc.getFilePath());
    }

    // ─── Yardımçı ─────────────────────────────────────────────────────────────

    private Customer findOrThrow(Long id) {
        return customerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Müştəri", id));
    }

    private Customer toEntity(CustomerRequest r, Customer c) {
        c.setCompanyName(r.getCompanyName());
        c.setVoen(r.getVoen());
        c.setAddress(r.getAddress());
        c.setSupplierPerson(r.getSupplierPerson());
        c.setSupplierPhone(r.getSupplierPhone());
        c.setOfficeContactPerson(r.getOfficeContactPerson());
        c.setOfficeContactPhone(r.getOfficeContactPhone());
        c.setPaymentTypes(r.getPaymentTypes() != null ? r.getPaymentTypes() : new java.util.HashSet<>());
        c.setStatus(r.getStatus() != null ? r.getStatus() : com.ces.erp.enums.CustomerStatus.ACTIVE);
        c.setRiskLevel(r.getRiskLevel() != null ? r.getRiskLevel() : com.ces.erp.enums.RiskLevel.LOW);
        c.setNotes(r.getNotes());
        return c;
    }
}
