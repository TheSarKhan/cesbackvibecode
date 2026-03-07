package com.ces.erp.customer.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

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
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = findOrThrow(id);
        if (request.getVoen() != null && !request.getVoen().isBlank()
                && customerRepository.existsByVoenAndIdNotAndDeletedFalse(request.getVoen(), id)) {
            throw new BusinessException("Bu VÖEN artıq qeydiyyatdadır");
        }
        return CustomerResponse.from(customerRepository.save(toEntity(request, customer)));
    }

    @Transactional
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
