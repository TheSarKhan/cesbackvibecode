package com.ces.erp.hr.service;

import com.ces.erp.approval.annotation.RequiresApproval;
import com.ces.erp.approval.context.ApprovalContext;
import com.ces.erp.approval.handler.ApprovalHandler;
import com.ces.erp.common.audit.AuditService;
import com.ces.erp.common.dto.PagedResponse;
import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.enums.EmployeeStatus;
import com.ces.erp.enums.LeaveStatus;
import com.ces.erp.enums.LeaveType;
import com.ces.erp.hr.dto.LeaveRequestDto;
import com.ces.erp.hr.dto.LeaveRequestResponse;
import com.ces.erp.hr.entity.Employee;
import com.ces.erp.hr.entity.LeaveRequest;
import com.ces.erp.hr.repository.EmployeeRepository;
import com.ces.erp.hr.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveService implements ApprovalHandler {

    private final LeaveRequestRepository repo;
    private final EmployeeRepository employeeRepository;
    private final AuditService auditService;

    @Override public String getEntityType() { return "LEAVE"; }
    @Override public String getModuleCode()  { return "HR"; }
    @Override public String getLabel(Long id) {
        LeaveRequest l = loadActive(id);
        Employee emp = l.getEmployee();
        return (emp != null ? emp.getFullName() : "Məzuniyyət") + " — " + l.getStartDate() + " → " + l.getEndDate();
    }
    @Override public Object getSnapshot(Long id) { return LeaveRequestResponse.from(loadActive(id)); }

    @Override
    public void applyEdit(Long id, String json) {
        // LeaveRequest üçün açıq edit endpoint yoxdur; pattern uyğunluğu üçün boş saxlanılır.
        throw new BusinessException("Məzuniyyət üçün edit dəstəklənmir");
    }

    @Override
    public void applyDelete(Long id) {
        ApprovalContext.setApplying(true);
        try { delete(id); } finally { ApprovalContext.clear(); }
    }

    public PagedResponse<LeaveRequestResponse> getPaged(int page, int size, Long employeeId, LeaveStatus status) {
        Page<LeaveRequest> result = repo.searchPaged(employeeId, status, PageRequest.of(page, size));
        return PagedResponse.<LeaveRequestResponse>builder()
                .content(result.getContent().stream().map(LeaveRequestResponse::from).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    public List<LeaveRequestResponse> getByEmployee(Long employeeId) {
        return repo.findAllByEmployeeIdAndDeletedFalseOrderByStartDateDesc(employeeId).stream()
                .map(LeaveRequestResponse::from)
                .toList();
    }

    public LeaveRequestResponse getById(Long id) {
        return LeaveRequestResponse.from(loadActive(id));
    }

    @Transactional
    public LeaveRequestResponse create(LeaveRequestDto req) {
        Employee emp = employeeRepository.findByIdAndDeletedFalse(req.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("İşçi", req.getEmployeeId()));
        if (emp.getStatus() == EmployeeStatus.TERMINATED) {
            throw new BusinessException("İşdən çıxmış işçi üçün məzuniyyət olmaz");
        }
        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new BusinessException("Bitmə tarixi başlama tarixindən əvvəl ola bilməz");
        }
        int days = (int) (ChronoUnit.DAYS.between(req.getStartDate(), req.getEndDate()) + 1);

        // Yalnız ANNUAL üçün qalıq yoxlanışı
        if (req.getType() == LeaveType.ANNUAL) {
            int used = repo.sumApprovedAnnualLeaveDaysInYear(emp.getId(), req.getStartDate().getYear());
            int allowance = emp.getAnnualLeaveDays() != null ? emp.getAnnualLeaveDays() : 21;
            if (used + days > allowance) {
                throw new BusinessException(
                        "İllik məzuniyyət limiti aşılır. Limit: " + allowance + " gün, istifadə: " + used + ", tələb: " + days);
            }
        }

        LeaveRequest l = LeaveRequest.builder()
                .employee(emp)
                .type(req.getType())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .days(days)
                .reason(req.getReason())
                .status(LeaveStatus.PENDING)
                .build();
        LeaveRequest saved = repo.save(l);
        auditService.log("HR_MƏZUNİYYƏT", saved.getId(), emp.getFullName(), "TƏLƏB_EDİLDİ",
                req.getType() + " — " + days + " gün");
        return LeaveRequestResponse.from(saved);
    }

    @Transactional
    public LeaveRequestResponse approve(Long id, String note) {
        LeaveRequest l = loadActive(id);
        if (l.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessException("Yalnız gözləyən tələblər təsdiqlənə bilər");
        }
        l.setStatus(LeaveStatus.APPROVED);
        l.setDecisionNote(note);
        l.setDecidedBy(currentUser());
        l.setDecidedAt(LocalDateTime.now());

        // İşçinin statusunu yenilə
        Employee emp = l.getEmployee();
        LocalDate today = LocalDate.now();
        if (!l.getStartDate().isAfter(today) && !l.getEndDate().isBefore(today)) {
            emp.setStatus(EmployeeStatus.ON_LEAVE);
            employeeRepository.save(emp);
        }
        repo.save(l);
        auditService.log("HR_MƏZUNİYYƏT", l.getId(), emp.getFullName(), "TƏSDİQLƏNDİ", note);
        return LeaveRequestResponse.from(l);
    }

    @Transactional
    public LeaveRequestResponse reject(Long id, String note) {
        LeaveRequest l = loadActive(id);
        if (l.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessException("Yalnız gözləyən tələblər rədd edilə bilər");
        }
        l.setStatus(LeaveStatus.REJECTED);
        l.setDecisionNote(note);
        l.setDecidedBy(currentUser());
        l.setDecidedAt(LocalDateTime.now());
        repo.save(l);
        auditService.log("HR_MƏZUNİYYƏT", l.getId(), l.getEmployee().getFullName(), "RƏDD_EDİLDİ", note);
        return LeaveRequestResponse.from(l);
    }

    @Transactional
    public LeaveRequestResponse cancel(Long id) {
        LeaveRequest l = loadActive(id);
        if (l.getStatus() == LeaveStatus.APPROVED && l.getStartDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Başlamış məzuniyyəti ləğv etmək olmaz");
        }
        l.setStatus(LeaveStatus.CANCELLED);
        repo.save(l);
        return LeaveRequestResponse.from(l);
    }

    @Transactional
    @RequiresApproval(module = "HR", entityType = "LEAVE", isDelete = true)
    public void delete(Long id) {
        LeaveRequest l = loadActive(id);
        l.softDelete();
        repo.save(l);
    }

    private LeaveRequest loadActive(Long id) {
        return repo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Məzuniyyət tələbi", id));
    }

    private static String currentUser() {
        try {
            Authentication a = SecurityContextHolder.getContext().getAuthentication();
            return a != null ? a.getName() : "system";
        } catch (Exception e) {
            return "system";
        }
    }
}
