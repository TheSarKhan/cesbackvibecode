package com.ces.erp.common.trash.service;

import com.ces.erp.accounting.repository.InvoiceRepository;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.common.trash.dto.TrashItem;
import com.ces.erp.contractor.repository.ContractorRepository;
import com.ces.erp.customer.repository.CustomerRepository;
import com.ces.erp.department.repository.DepartmentRepository;
import com.ces.erp.garage.repository.EquipmentRepository;
import com.ces.erp.investor.repository.InvestorRepository;
import com.ces.erp.operator.repository.OperatorRepository;
import com.ces.erp.project.repository.ProjectRepository;
import com.ces.erp.request.repository.TechRequestRepository;
import com.ces.erp.role.repository.RoleRepository;
import com.ces.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TrashService {

    private final CustomerRepository customerRepository;
    private final ContractorRepository contractorRepository;
    private final InvestorRepository investorRepository;
    private final OperatorRepository operatorRepository;
    private final EquipmentRepository equipmentRepository;
    private final TechRequestRepository techRequestRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final InvoiceRepository invoiceRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Transactional(readOnly = true)
    public List<TrashItem> getAll(String moduleCode) {
        List<TrashItem> items = new ArrayList<>();

        if (moduleCode == null || "CUSTOMER_MANAGEMENT".equals(moduleCode)) {
            customerRepository.findAllByDeletedTrue().forEach(e -> {
                Map<String, String> d = new LinkedHashMap<>();
                d.put("Şirkət adı", e.getCompanyName());
                d.put("VÖEN", nvl(e.getVoen()));
                d.put("Ünvan", nvl(e.getAddress()));
                d.put("Təchizat məsul şəxsi", nvl(e.getSupplierPerson()));
                d.put("Təchizat telefonu", nvl(e.getSupplierPhone()));
                d.put("Ofis əlaqə şəxsi", nvl(e.getOfficeContactPerson()));
                d.put("Ofis telefonu", nvl(e.getOfficeContactPhone()));
                d.put("Ödəniş növü", e.getPaymentTypes() != null ? String.join(", ", e.getPaymentTypes()) : "—");
                d.put("Status", nvl(e.getStatus()));
                d.put("Risk səviyyəsi", nvl(e.getRiskLevel()));
                d.put("Qeydlər", nvl(e.getNotes()));
                items.add(item(e.getId(), "CUSTOMER", e.getCompanyName(), "CUSTOMER_MANAGEMENT", "Müştəri İdarəetməsi", e.getDeletedAt(), d));
            });
        }
        if (moduleCode == null || "CONTRACTOR_MANAGEMENT".equals(moduleCode)) {
            contractorRepository.findAllByDeletedTrue().forEach(e -> {
                Map<String, String> d = new LinkedHashMap<>();
                d.put("Şirkət adı", e.getCompanyName());
                d.put("VÖEN", nvl(e.getVoen()));
                d.put("Əlaqə şəxsi", nvl(e.getContactPerson()));
                d.put("Telefon", nvl(e.getPhone()));
                d.put("Ünvan", nvl(e.getAddress()));
                d.put("Ödəniş növü", nvl(e.getPaymentType()));
                d.put("Status", nvl(e.getStatus()));
                d.put("Reytinq", nvl(e.getRating()));
                d.put("Risk səviyyəsi", nvl(e.getRiskLevel()));
                d.put("Qeydlər", nvl(e.getNotes()));
                items.add(item(e.getId(), "CONTRACTOR", e.getCompanyName(), "CONTRACTOR_MANAGEMENT", "Podratçı İdarəetməsi", e.getDeletedAt(), d));
            });
        }
        if (moduleCode == null || "INVESTORS".equals(moduleCode)) {
            investorRepository.findAllByDeletedTrue().forEach(e -> {
                Map<String, String> d = new LinkedHashMap<>();
                d.put("Şirkət adı", e.getCompanyName());
                d.put("VÖEN", nvl(e.getVoen()));
                d.put("Əlaqə şəxsi", nvl(e.getContactPerson()));
                d.put("Telefon", nvl(e.getContactPhone()));
                d.put("Ünvan", nvl(e.getAddress()));
                d.put("Ödəniş növü", nvl(e.getPaymentType()));
                d.put("Status", nvl(e.getStatus()));
                d.put("Reytinq", nvl(e.getRating()));
                d.put("Risk səviyyəsi", nvl(e.getRiskLevel()));
                d.put("Qeydlər", nvl(e.getNotes()));
                items.add(item(e.getId(), "INVESTOR", e.getCompanyName(), "INVESTORS", "İnvestor İdarəetməsi", e.getDeletedAt(), d));
            });
        }
        if (moduleCode == null || "OPERATORS".equals(moduleCode)) {
            operatorRepository.findAllByDeletedTrue().forEach(e -> {
                Map<String, String> d = new LinkedHashMap<>();
                d.put("Ad", e.getFirstName());
                d.put("Soyad", e.getLastName());
                d.put("Telefon", nvl(e.getPhone()));
                d.put("Email", nvl(e.getEmail()));
                d.put("Ünvan", nvl(e.getAddress()));
                d.put("İxtisas", nvl(e.getSpecialization()));
                d.put("Qeydlər", nvl(e.getNotes()));
                items.add(item(e.getId(), "OPERATOR", e.getFirstName() + " " + e.getLastName(), "OPERATORS", "Operator İdarəetməsi", e.getDeletedAt(), d));
            });
        }
        if (moduleCode == null || "GARAGE".equals(moduleCode)) {
            equipmentRepository.findAllByDeletedTrue().forEach(e -> {
                Map<String, String> d = new LinkedHashMap<>();
                d.put("Ad", e.getName());
                d.put("Kod", e.getEquipmentCode());
                d.put("Növ", e.getType());
                d.put("Seriya nömrəsi", nvl(e.getSerialNumber()));
                d.put("Marka", nvl(e.getBrand()));
                d.put("Model", nvl(e.getModel()));
                d.put("İstehsal ili", nvl(e.getManufactureYear()));
                d.put("Alış tarixi", e.getPurchaseDate() != null ? e.getPurchaseDate().format(DATE_FMT) : "—");
                d.put("Alış qiyməti", nvl(e.getPurchasePrice()));
                d.put("Dövlət nişanı", nvl(e.getPlateNumber()));
                d.put("Çəki (ton)", nvl(e.getWeightTon()));
                d.put("Bazar dəyəri", nvl(e.getCurrentMarketValue()));
                d.put("Saxlama yeri", nvl(e.getStorageLocation()));
                d.put("Mülkiyyət növü", nvl(e.getOwnershipType()));
                d.put("Status", nvl(e.getStatus()));
                d.put("Qeydlər", nvl(e.getNotes()));
                items.add(item(e.getId(), "EQUIPMENT", e.getName() + " (" + e.getEquipmentCode() + ")", "GARAGE", "Qaraj Modulu", e.getDeletedAt(), d));
            });
        }
        if (moduleCode == null || "REQUESTS".equals(moduleCode)) {
            techRequestRepository.findAllByDeletedTrue().forEach(e -> {
                Map<String, String> d = new LinkedHashMap<>();
                d.put("Sorğu kodu", nvl(e.getRequestCode()));
                d.put("Şirkət", nvl(e.getCompanyName()));
                d.put("Əlaqə şəxsi", nvl(e.getContactPerson()));
                d.put("Telefon", nvl(e.getContactPhone()));
                d.put("Status", nvl(e.getStatus()));
                d.put("Yaradılma tarixi", e.getCreatedAt() != null ? e.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "—");
                items.add(item(e.getId(), "TECH_REQUEST",
                    e.getRequestCode() != null ? e.getRequestCode() : "Sorğu #" + e.getId(),
                    "REQUESTS", "Sorğular Modulu", e.getDeletedAt(), d));
            });
        }
        if (moduleCode == null || "PROJECTS".equals(moduleCode)) {
            projectRepository.findAllByDeletedTrue().forEach(e -> {
                Map<String, String> d = new LinkedHashMap<>();
                d.put("Layihə kodu", e.getProjectCode());
                d.put("Status", nvl(e.getStatus()));
                d.put("Başlama tarixi", e.getStartDate() != null ? e.getStartDate().format(DATE_FMT) : "—");
                d.put("Bitmə tarixi", e.getEndDate() != null ? e.getEndDate().format(DATE_FMT) : "—");
                d.put("Müqavilə", e.isHasContract() ? "Var" : "Yoxdur");
                d.put("Plan saatları", nvl(e.getScheduledHours()));
                d.put("Faktiki saatlar", nvl(e.getActualHours()));
                items.add(item(e.getId(), "PROJECT", e.getProjectCode(), "PROJECTS", "Layihələr Modulu", e.getDeletedAt(), d));
            });
        }
        if (moduleCode == null || "EMPLOYEE_MANAGEMENT".equals(moduleCode)) {
            userRepository.findAllByDeletedTrue().forEach(e -> {
                Map<String, String> d = new LinkedHashMap<>();
                d.put("Ad Soyad", e.getFullName());
                d.put("Email", e.getEmail());
                d.put("Telefon", nvl(e.getPhone()));
                d.put("Şöbə", e.getDepartment() != null ? e.getDepartment().getName() : "—");
                d.put("Rol", e.getRole() != null ? e.getRole().getName() : "—");
                d.put("Aktiv", e.isActive() ? "Bəli" : "Xeyr");
                items.add(item(e.getId(), "USER", e.getFullName(), "EMPLOYEE_MANAGEMENT", "İstifadəçi İdarəetməsi", e.getDeletedAt(), d));
            });
            departmentRepository.findAllByDeletedTrue().forEach(e -> {
                Map<String, String> d = new LinkedHashMap<>();
                d.put("Ad", e.getName());
                d.put("Təsvir", nvl(e.getDescription()));
                items.add(item(e.getId(), "DEPARTMENT", e.getName(), "EMPLOYEE_MANAGEMENT", "İstifadəçi İdarəetməsi", e.getDeletedAt(), d));
            });
        }
        if (moduleCode == null || "ROLE_PERMISSION".equals(moduleCode)) {
            roleRepository.findAllByDeletedTrue().forEach(e -> {
                Map<String, String> d = new LinkedHashMap<>();
                d.put("Ad", e.getName());
                d.put("Təsvir", nvl(e.getDescription()));
                d.put("Şöbə", e.getDepartment() != null ? e.getDepartment().getName() : "—");
                items.add(item(e.getId(), "ROLE", e.getName(), "ROLE_PERMISSION", "Rol və İcazə İdarəetməsi", e.getDeletedAt(), d));
            });
        }
        if (moduleCode == null || "ACCOUNTING".equals(moduleCode)) {
            invoiceRepository.findAllByDeletedTrue().forEach(e -> {
                Map<String, String> d = new LinkedHashMap<>();
                d.put("Qaimə №", nvl(e.getInvoiceNumber()));
                d.put("Növ", nvl(e.getType()));
                d.put("Məbləğ", nvl(e.getAmount()));
                d.put("Tarix", e.getInvoiceDate() != null ? e.getInvoiceDate().format(DATE_FMT) : "—");
                d.put("Şirkət", nvl(e.getCompanyName()));
                d.put("Texnika", nvl(e.getEquipmentName()));
                d.put("ETaxes ID", nvl(e.getEtaxesId()));
                d.put("Xidmət təsviri", nvl(e.getServiceDescription()));
                d.put("Layihə", e.getProject() != null ? e.getProject().getProjectCode() : "—");
                d.put("Podratçı", e.getContractor() != null ? e.getContractor().getCompanyName() : "—");
                d.put("Qeydlər", nvl(e.getNotes()));
                items.add(item(e.getId(), "INVOICE", nvl(e.getInvoiceNumber()), "ACCOUNTING", "Mühasibatlıq Modulu", e.getDeletedAt(), d));
            });
        }

        items.sort(Comparator.comparing(
            TrashItem::getDeletedAt,
            Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return items;
    }

    @Transactional
    public void restore(String entityType, Long id) {
        switch (entityType) {
            case "CUSTOMER" -> {
                var e = customerRepository.findById(id).filter(c -> c.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Müştəri", id));
                e.setDeleted(false); e.setDeletedAt(null); customerRepository.save(e);
            }
            case "CONTRACTOR" -> {
                var e = contractorRepository.findById(id).filter(c -> c.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Podratçı", id));
                e.setDeleted(false); e.setDeletedAt(null); contractorRepository.save(e);
            }
            case "INVESTOR" -> {
                var e = investorRepository.findById(id).filter(c -> c.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("İnvestor", id));
                e.setDeleted(false); e.setDeletedAt(null); investorRepository.save(e);
            }
            case "OPERATOR" -> {
                var e = operatorRepository.findById(id).filter(c -> c.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Operator", id));
                e.setDeleted(false); e.setDeletedAt(null); operatorRepository.save(e);
            }
            case "EQUIPMENT" -> {
                var e = equipmentRepository.findById(id).filter(c -> c.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Texnika", id));
                e.setDeleted(false); e.setDeletedAt(null); equipmentRepository.save(e);
            }
            case "TECH_REQUEST" -> {
                var e = techRequestRepository.findById(id).filter(c -> c.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Sorğu", id));
                e.setDeleted(false); e.setDeletedAt(null); techRequestRepository.save(e);
            }
            case "PROJECT" -> {
                var e = projectRepository.findById(id).filter(c -> c.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Layihə", id));
                e.setDeleted(false); e.setDeletedAt(null); projectRepository.save(e);
            }
            case "USER" -> {
                var e = userRepository.findById(id).filter(c -> c.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi", id));
                e.setDeleted(false); e.setDeletedAt(null); userRepository.save(e);
            }
            case "DEPARTMENT" -> {
                var e = departmentRepository.findById(id).filter(c -> c.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Şöbə", id));
                e.setDeleted(false); e.setDeletedAt(null); departmentRepository.save(e);
            }
            case "ROLE" -> {
                var e = roleRepository.findById(id).filter(c -> c.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Rol", id));
                e.setDeleted(false); e.setDeletedAt(null); roleRepository.save(e);
            }
            case "INVOICE" -> {
                var e = invoiceRepository.findById(id).filter(c -> c.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("İnvoice", id));
                e.setDeleted(false); e.setDeletedAt(null); invoiceRepository.save(e);
            }
            default -> throw new BusinessException("Naməlum entity tipi: " + entityType);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private TrashItem item(Long id, String entityType, String label,
                           String moduleCode, String moduleName,
                           java.time.LocalDateTime deletedAt, Map<String, String> details) {
        return TrashItem.builder()
            .id(id).entityType(entityType).entityLabel(label)
            .moduleCode(moduleCode).moduleName(moduleName)
            .deletedAt(deletedAt).details(details)
            .build();
    }

    private String nvl(Object val) {
        if (val == null) return "—";
        String s = val.toString().trim();
        return s.isEmpty() ? "—" : s;
    }
}
